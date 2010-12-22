/**
 * 
 */
package org.yocto.bc.ui.wizards.install;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.yocto.bc.bitbake.ICommandResponseHandler;
import org.yocto.bc.bitbake.ShellSession;
import org.yocto.bc.ui.Activator;

class InstallJob extends Job {

	private final Map mod;
	private UICommandResponseHandler cmdOut;
	private boolean errorOccurred = false;

	public InstallJob(Map model, ProgressPage progressPage) {
		super("Install Yocto");
		mod = model;
		cmdOut = new UICommandResponseHandler(progressPage);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		BufferedReader reader = new BufferedReader(new StringReader((String) mod.get(FlavorPage.INSTALL_SCRIPT)));
		String line = null;
		Map vars = loadVariables();

		try {
			ShellSession shell = new ShellSession(ShellSession.SHELL_TYPE_BASH, null, null, null);
			while ((line = reader.readLine()) != null && !errorOccurred) {
				line = line.trim();
				if (line.length() > 0 && !line.startsWith("#")) {
					line = substitute(line, vars);
					cmdOut.printCmd(line);
					shell.execute(line, cmdOut);
				} else if (line.startsWith("#")) {
					cmdOut.printDialog(line.substring(1).trim());
				}
			}

			if (errorOccurred) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to install OpenEmbedded");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to install OpenEmbedded", e);
		}

		return Status.OK_STATUS;
	}

	private Map loadVariables() {
		//return (Map) mod.get(OptionsPage.OPTION_MAP);
		return (Map) mod;
	}
	

	/**
	 * Return a string with variable substitutions in place.
	 * 
	 * @param expression
	 * @return Input string with any substitutions from this file.
	 */
	public static String substitute(String expression, Map mo) {

		List vars = parseVars(expression);

		for (Iterator i = vars.iterator(); i.hasNext();) {
			String varName = (String) i.next();
			String varToken = "${" + varName + "}";

			if (mo.containsKey(varName)) {
				expression = expression.replace(varToken, (String) mo.get(varName));
			} else if (System.getProperty(varName) != null) {
				expression = expression.replace(varToken, System.getProperty(varName));
			} else if (varName.toUpperCase().equals("HOME")) {
				expression = expression.replace(varToken, System.getProperty("user.home"));
			}
		}

		return expression;
	}

	/**
	 * 
	 * @param line
	 * @return A list of variables in $[variable name] format.
	 */
	public static List parseVars(String line) {
		List l = new ArrayList();

		int i = 0;

		while ((i = line.indexOf("${", i)) > -1) {
			int i2 = line.indexOf("}", i);

			String var = line.subSequence(i + 2, i2).toString().trim();

			if (var.length() > 0 && !l.contains(var)) {
				l.add(var);
			}
			i++;
		}

		return l;
	}

	private class UICommandResponseHandler implements ICommandResponseHandler {

		private final ProgressPage progressPage;

		public UICommandResponseHandler(ProgressPage progressPage) {
			this.progressPage = progressPage;
		}

		public void printDialog(String msg) {
			progressPage.printDialog(msg);
		}

		public void response(String line, boolean isError) {
			if (isError) {
				progressPage.printLine(line, ProgressPage.PRINT_ERR);
				errorOccurred = true;
			} else {
				progressPage.printLine(line, ProgressPage.PRINT_OUT);
			}
		}

		public void printCmd(String cmd) {
			progressPage.printLine(cmd, ProgressPage.PRINT_CMD);
		}

	}

}
