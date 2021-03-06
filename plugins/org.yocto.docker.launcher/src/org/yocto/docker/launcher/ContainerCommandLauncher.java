package org.yocto.docker.launcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.docker.launcher.DockerLaunchUIPlugin;
import org.eclipse.cdt.internal.core.ProcessClosure;
import org.eclipse.cdt.internal.docker.launcher.Messages;
import org.eclipse.cdt.internal.docker.launcher.PreferenceConstants;
import org.eclipse.cdt.managedbuilder.buildproperties.IOptionalBuildProperties;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.linuxtools.docker.ui.launch.IErrorMessageHolder;
import org.eclipse.linuxtools.internal.docker.ui.launch.ContainerCommandProcess;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.Preferences;

/**
 * Forked from ContainerCommandLauncher
 *
 * @see org.eclipse.cdt.docker.launcher.ContainerCommandLauncher
 *
 */
@SuppressWarnings("restriction")
public class ContainerCommandLauncher implements ICommandLauncher, IErrorMessageHolder {

	public final static String CONTAINER_BUILD_ENABLED = DockerLaunchUIPlugin.PLUGIN_ID
			+ ".containerbuild.property.enablement"; //$NON-NLS-1$
	public final static String CONNECTION_ID = DockerLaunchUIPlugin.PLUGIN_ID + ".containerbuild.property.connection"; //$NON-NLS-1$
	public final static String IMAGE_ID = DockerLaunchUIPlugin.PLUGIN_ID + ".containerbuild.property.image"; //$NON-NLS-1$
	public final static String VOLUMES_ID = DockerLaunchUIPlugin.PLUGIN_ID + ".containerbuild.property.volumes"; //$NON-NLS-1$
	public final static String SELECTED_VOLUMES_ID = DockerLaunchUIPlugin.PLUGIN_ID
			+ ".containerbuild.property.selectedvolumes"; //$NON-NLS-1$

	public final static String VOLUME_SEPARATOR_REGEX = "[|]"; //$NON-NLS-1$

	private IProject fProject;
	private Process fProcess;
	private boolean fShowCommand;
	private String fErrorMessage;
	private Properties fEnvironment;

	private String[] commandArgs;
	private String fImageName = ""; //$NON-NLS-1$

	public final static int COMMAND_CANCELED = ICommandLauncher.COMMAND_CANCELED;
	public final static int ILLEGAL_COMMAND = ICommandLauncher.ILLEGAL_COMMAND;
	public final static int OK = ICommandLauncher.OK;

	private static final String NEWLINE = System.getProperty("line.separator", //$NON-NLS-1$
			"\n"); //$NON-NLS-1$

	private static Map<IProject, Integer> uidMap = new HashMap<>();

	/**
	 * The number of milliseconds to pause between polling.
	 */
	protected static final long DELAY = 50L;

	@Override
	public void setProject(IProject project) {
		this.fProject = project;
	}

	@Override
	public IProject getProject() {
		return fProject;
	}

	@SuppressWarnings("unused")
	private String getImageName() {
		return fImageName;
	}

	private void setImageName(String imageName) {
		fImageName = imageName;
	}

	@Override
	public void showCommand(boolean show) {
		this.fShowCommand = show;
	}

	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	@Override
	public void setErrorMessage(String error) {
		fErrorMessage = error;
	}

	@Override
	public String[] getCommandArgs() {
		return commandArgs;
	}

	@Override
	public Properties getEnvironment() {
		return fEnvironment;
	}

	@Override
	public String getCommandLine() {
		// TODO Auto-generated method stub
		return null;
	}

	protected Integer getUid() {

		String os = System.getProperty("os.name"); //$NON-NLS-1$
		if (os.indexOf("nux") > 0) { //$NON-NLS-1$
			// first try and see if we have already run a command on this
			// project
			Integer cachedUid = uidMap.get(fProject);
			if (cachedUid != null) {
				return cachedUid;
			}

			try {
				Integer resolvedUid = (Integer) Files.getAttribute(fProject.getLocation().toFile().toPath(),
						"unix:uid"); //$NON-NLS-1$
				// store the uid for possible later usage
				uidMap.put(fProject, resolvedUid);
				return resolvedUid;
			} catch (IOException e) {
				// do nothing...leave as null
			}
		}
		return null;
	}

	@Override
	public Process execute(IPath commandPath, String[] args, String[] env, IPath workingDirectory,
			IProgressMonitor monitor) throws CoreException {

		HashMap<String, String> labels = new HashMap<>();
		labels.put("org.eclipse.cdt.container-command", ""); //$NON-NLS-1$ //$NON-NLS-2$
		String projectName = fProject.getName();
		labels.put("org.eclipse.cdt.project-name", projectName); //$NON-NLS-1$

		List<String> additionalDirs = new ArrayList<>();

		//
		IPath projectLocation = fProject.getLocation();
		String projectPath = projectLocation.toPortableString();
		if (projectLocation.getDevice() != null) {
			projectPath = "/" + projectPath.replace(':', '/'); //$NON-NLS-1$
		}
		additionalDirs.add(projectPath);

		ArrayList<String> commandSegments = new ArrayList<>();

		StringBuilder b = new StringBuilder();
		String commandString = commandPath.toPortableString();
		if (commandPath.getDevice() != null) {
			commandString = "/" + commandString.replace(':', '/'); //$NON-NLS-1$
		}
		b.append(commandString);
		commandSegments.add(commandString);
		for (String arg : args) {
			b.append(" "); //$NON-NLS-1$
			String realArg = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(arg);
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				// check if file exists and if so, add an additional directory
				IPath p = new Path(realArg);
				if (p.isValidPath(realArg) && p.getDevice() != null) {
					File f = p.toFile();
					String modifiedArg = realArg;
					// if the directory of the arg as a file exists, we mount it
					// and modify the argument to be unix-style
					if (f.isFile()) {
						f = f.getParentFile();
						modifiedArg = "/" //$NON-NLS-1$
								+ p.toPortableString().replace(':', '/');
						p = p.removeLastSegments(1);
					}
					if (f != null && f.exists()) {
						additionalDirs.add("/" + p.toPortableString().replace(':', '/')); //$NON-NLS-1$
						realArg = modifiedArg;
					}
				}
			} else if (realArg.startsWith("/")) { //$NON-NLS-1$
				// check if file directory exists and if so, add an additional
				// directory
				IPath p = new Path(realArg);
				if (p.isValidPath(realArg)) {
					File f = p.toFile();
					if (f.isFile()) {
						f = f.getParentFile();
					}
					if (f != null && f.exists()) {
						additionalDirs.add(f.getAbsolutePath());
					}
				}
			}

			if (realArg.contains(" ")) { //$NON-NLS-1$
				b.append('"' + realArg + '"');
			} else {
				b.append(realArg);
			}
			commandSegments.add(realArg);
		}

		commandArgs = commandSegments.toArray(new String[0]);

		String commandDir = commandPath.removeLastSegments(1).toString();
		if (commandDir.isEmpty()) {
			commandDir = null;
		} else if (commandPath.getDevice() != null) {
			commandDir = "/" + commandDir.replace(':', '/'); //$NON-NLS-1$
		}

		IProject[] referencedProjects = fProject.getReferencedProjects();
		for (IProject referencedProject : referencedProjects) {
			String referencedProjectPath = referencedProject.getLocation().toPortableString();
			if (referencedProject.getLocation().getDevice() != null) {
				referencedProjectPath = "/" //$NON-NLS-1$
						+ referencedProjectPath.replace(':', '/');
			}
			additionalDirs.add(referencedProjectPath);
		}

		String command = b.toString();

		String workingDir = workingDirectory.makeAbsolute().toPortableString();
		if (workingDirectory.toPortableString().equals(".")) { //$NON-NLS-1$
			workingDir = "/tmp"; //$NON-NLS-1$
		} else if (workingDirectory.getDevice() != null) {
			workingDir = "/" + workingDir.replace(':', '/'); //$NON-NLS-1$
		}
		parseEnvironment(env);
		Map<String, String> origEnv = null;

		boolean supportStdin = false;

		boolean privilegedMode = false;

		Preferences prefs = InstanceScope.INSTANCE.getNode(DockerLaunchUIPlugin.PLUGIN_ID);

		boolean keepContainer = prefs.getBoolean(PreferenceConstants.KEEP_CONTAINER_AFTER_LAUNCH, false);

		ICConfigurationDescription cfgd = CoreModel.getDefault().getProjectDescription(fProject)
				.getActiveConfiguration();
		IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(cfgd);
		if (cfg == null) {
			return null;
		}
		IOptionalBuildProperties props = cfg.getOptionalBuildProperties();

		// Add any specified volumes to additional dir list
		String selectedVolumeString = props.getProperty(SELECTED_VOLUMES_ID);
		if (selectedVolumeString != null && !selectedVolumeString.isEmpty()) {
			String[] selectedVolumes = selectedVolumeString.split(VOLUME_SEPARATOR_REGEX);
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				for (String selectedVolume : selectedVolumes) {
					IPath path = new Path(selectedVolume);
					String selectedPath = path.toPortableString();
					if (path.getDevice() != null) {
						selectedPath = "/" + selectedPath.replace(':', '/'); //$NON-NLS-1$
					}
					additionalDirs.add(selectedPath);
				}
			} else {
				additionalDirs.addAll(Arrays.asList(selectedVolumes));
			}
		}

		String connectionName = props.getProperty(ContainerCommandLauncher.CONNECTION_ID);
		if (connectionName == null) {
			return null;
		}
		String imageName = props.getProperty(ContainerCommandLauncher.IMAGE_ID);
		if (imageName == null) {
			return null;
		}
		setImageName(imageName);

		Integer uid = getUid();

		fProcess = getContainerLauncher().runCommand(connectionName, imageName, fProject, this, command, commandDir,
				workingDir, additionalDirs, origEnv, fEnvironment, supportStdin, privilegedMode, labels, keepContainer,
				uid);

		return fProcess;
	}

	protected ContainerLauncher getContainerLauncher() {
		return new ContainerLauncher();
	}

	/**
	 * Parse array of "ENV=value" pairs to Properties.
	 */
	private void parseEnvironment(String[] env) {
		fEnvironment = null;
		if (env != null) {
			fEnvironment = new Properties();
			for (String envStr : env) {
				// Split "ENV=value" and put in Properties
				int pos = envStr.indexOf('='); // $NON-NLS-1$
				if (pos < 0)
					pos = envStr.length();
				String key = envStr.substring(0, pos);
				String value = envStr.substring(pos + 1);
				fEnvironment.put(key, value);
			}
		}
	}

	@Override
	public int waitAndRead(OutputStream out, OutputStream err) {
		printImageHeader(out);

		if (fShowCommand) {
			printCommandLine(out);
		}

		if (fProcess == null) {
			return ILLEGAL_COMMAND;
		}
		ProcessClosure closure = new ProcessClosure(fProcess, out, err);
		closure.runBlocking(); // a blocking call
		return OK;
	}

	@Override
	public int waitAndRead(OutputStream output, OutputStream err, IProgressMonitor monitor) {
		printImageHeader(output);

		if (fShowCommand) {
			printCommandLine(output);
		}

		if (fProcess == null) {
			return ILLEGAL_COMMAND;
		}

		ProcessClosure closure = new ProcessClosure(fProcess, output, err);
		closure.runNonBlocking();
		Runnable watchProcess = () -> {
			try {
				fProcess.waitFor();
			} catch (InterruptedException e) {
				// ignore
			}
			closure.terminate();
		};
		Thread t = new Thread(watchProcess);
		t.start();
		while (!monitor.isCanceled() && closure.isAlive()) {
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException ie) {
				break;
			}
		}
		try {
			t.join(500);
		} catch (InterruptedException e1) {
			// ignore
		}
		int state = OK;

		// Operation canceled by the user, terminate abnormally.
		if (monitor.isCanceled()) {
			closure.terminate();
			state = COMMAND_CANCELED;
			setErrorMessage(Messages.CommandLauncher_CommandCancelled);
		}
		try {
			fProcess.waitFor();
		} catch (InterruptedException e) {
			// ignore
		}

		monitor.done();
		return state;
	}

	protected void printImageHeader(OutputStream os) {
		if (os != null) {
			try {
				os.write(NLS.bind(Messages.ContainerCommandLauncher_image_msg,
						((ContainerCommandProcess) fProcess).getImage()).getBytes());
				os.write(NEWLINE.getBytes());
				os.flush();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	protected void printCommandLine(OutputStream os) {
		if (os != null) {
			try {
				os.write(getCommandLineQuoted(getCommandArgs(), true).getBytes());
				os.flush();
			} catch (IOException e) {
				// ignore;
			}
		}
	}

	@SuppressWarnings("nls")
	private String getCommandLineQuoted(String[] commandArgs, boolean quote) {
		StringBuilder buf = new StringBuilder();
		if (commandArgs != null) {
			for (String commandArg : commandArgs) {
				if (quote && (commandArg.contains(" ") || commandArg.contains("\"") || commandArg.contains("\\"))) {
					commandArg = '"' + commandArg.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + '"';
				}
				buf.append(commandArg);
				buf.append(' ');
			}
			buf.append(NEWLINE);
		}
		return buf.toString();
	}

	protected String getCommandLine(String[] commandArgs) {
		return getCommandLineQuoted(commandArgs, false);
	}

}
