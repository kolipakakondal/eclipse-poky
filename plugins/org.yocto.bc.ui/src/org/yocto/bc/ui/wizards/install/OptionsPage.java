package org.yocto.bc.ui.wizards.install;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import org.yocto.bc.ui.wizards.FiniteStateWizardPage;

/**
 * Select which flavor of OE is to be installed.
 * 
 * @author kgilmer
 * 
 */
public class OptionsPage extends FiniteStateWizardPage {

	private Map vars;
	private Composite c1;
	private Composite top;
	
	private List controlList;
	private boolean controlsCreated = false;

	protected OptionsPage(Map model) {
		super("Options", model);
		setTitle("Installing...");
		setMessage("Enter these parameters to install.");
	}

	@Override
	public void createControl(Composite parent) {
		top = new Composite(parent, SWT.None);
		top.setLayout(new GridLayout());
		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		c1 = new Composite(top, SWT.None);
		c1.setLayout(new GridLayout(2, false));
		c1.setLayoutData(new GridData(GridData.FILL_BOTH));

		setControl(top);
	}

	//private void createControls(Composite comp, Map v, List cl) {
	private void createControls(Composite comp, List cl) {
		ValidationListener listener = new ValidationListener();
		
		//String label = InstallWizard.INSTALL_DIRECTORY;
		(new Label(comp, SWT.None)).setText(InstallWizard.INSTALL_DIRECTORY + ": ");
		Composite locComposite = new Composite(comp, SWT.NONE);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_END
				| GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 0;
		locComposite.setLayoutData(gd);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		locComposite.setLayout(gl);

		final Text location = new Text(locComposite, SWT.BORDER);
		location.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		location.setText((String)model.get(InstallWizard.INSTALL_DIRECTORY));
		location.addModifyListener(listener);
		//location..setData(ip);
		cl.add(location);

		Button button = new Button(locComposite, SWT.PUSH);
		button.setText("...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(PlatformUI.getWorkbench()
						.getDisplay().getActiveShell(), SWT.OPEN);
				
				fd.setText(InstallWizard.INSTALL_DIRECTORY);

				String selected = fd.open();

				if (selected != null) {
					location.setText(selected);
					//updateModel();
				}
			}
		});
		
		(new Label(comp, SWT.None)).setText(InstallWizard.INIT_SCRIPT + ": ");
		Text field = new Text(comp, SWT.BORDER);
		//field.setData(ip);
		field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		field.setText((String)model.get(InstallWizard.INIT_SCRIPT));
		field.addModifyListener(listener);
		cl.add(field);
		
/*
		for (Iterator i = v.keySet().iterator(); i.hasNext();) {
			
			String label = (String) i.next();
			final InstallParameter ip = (InstallParameter) v.get(label);

			(new Label(comp, SWT.None)).setText(ip.getLabel() + ": ");
			
			switch (ip.type) {
			case InstallParameter.DT_TEXT:
				Text field = new Text(comp, SWT.BORDER);
				field.setData(ip);
				field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				field.addModifyListener(listener);
				field.setText(ip.getData());
				cl.add(field);
				break;
			case InstallParameter.DT_DIRECTORY:
				Composite locComposite = new Composite(comp, SWT.NONE);
				GridData gd = new GridData(GridData.VERTICAL_ALIGN_END
						| GridData.FILL_HORIZONTAL);
				gd.horizontalIndent = 0;
				locComposite.setLayoutData(gd);
				GridLayout gl = new GridLayout(2, false);
				gl.marginWidth = 0;
				locComposite.setLayout(gl);

				final Text location = new Text(locComposite, SWT.BORDER);
				location.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				location.setText(ip.getData());
				location.addModifyListener(listener);
				location.setData(ip);
				cl.add(location);

				Button button = new Button(locComposite, SWT.PUSH);
				button.setText("...");
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						DirectoryDialog fd = new DirectoryDialog(PlatformUI.getWorkbench()
								.getDisplay().getActiveShell(), SWT.OPEN);
						
						fd.setText(ip.getLabel());

						String selected = fd.open();

						if (selected != null) {
							location.setText(selected);
							//updateModel();
						}
					}
				});
				break;
			case InstallParameter.DT_FILE:
				Composite fileComposite = new Composite(comp, SWT.NONE);
				gd = new GridData(GridData.VERTICAL_ALIGN_END
						| GridData.FILL_HORIZONTAL);
				gd.horizontalIndent = 0;
				fileComposite.setLayoutData(gd);
				gl = new GridLayout(2, false);
				gl.marginWidth = 0;
				fileComposite.setLayout(gl);

				final Text fileLocation = new Text(fileComposite, SWT.BORDER);
				fileLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fileLocation.setText(ip.getData());
				fileLocation.addModifyListener(listener);
				fileLocation.setData(ip);
				cl.add(fileLocation);

				button = new Button(fileComposite, SWT.PUSH);
				button.setText("...");
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fd = new FileDialog(PlatformUI.getWorkbench()
								.getDisplay().getActiveShell(), SWT.OPEN);
						
						fd.setText(ip.getLabel());

						String selected = fd.open();

						if (selected != null) {
							fileLocation.setText(selected);
							//updateModel();
						}
					}
				});
				break;
			default:
				throw new RuntimeException("Unknown or unimplemented field: " + ip.type);
			}
		}
		*/
	}

	@Override
	public void pageCleanup() {

	}

	@Override
	public void pageDisplay() {
		if (!controlsCreated) {
			controlList = new ArrayList();
			//model.put(INSTALL_SCRIPT, InstallScriptHelper.loadFile(f.getScriptURL()));
			//vars = parseVars((String) model.get(FlavorPage.INSTALL_SCRIPT));
			//try {
				//vars = parseVars((String) InstallScriptHelper.loadFile("scripts/poky_install.sh"));
				//vars = parseVars((String) model.get(INSTALL_SCRIPT));
				//createControls(c1, vars, controlList);
				createControls(c1, controlList);
				c1.layout();

				controlsCreated = true;
				//setTitle(((String) model.get(FlavorPage.OE_FLAVOR_TITLE)).trim() + " Options");
				setTitle("Create from git repository options");
			//} catch (IOException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
		}
	}

	public static Map parseVars(String line) {
		Map l = new Hashtable();

		int i = 0;

		while ((i = line.indexOf("{|", i)) > -1) {
			int i2 = line.indexOf("|}", i);

			String var = line.subSequence(i + 2, i2).toString().trim();

			if (var.length() > 0) {
				InstallParameter ip = new InstallParameter(var + " ");

				if (ip.isValid() && !l.containsKey(ip.getLabel())) {
					l.put(ip.getLabel(),ip);
				}
			}
			i++;
		}

		return l;
	}
	
	@Override
	
	protected void updateModel() {
		model.put(InstallWizard.INSTALL_DIRECTORY,((Text)controlList.get(0)).getText());
		model.put(InstallWizard.INIT_SCRIPT,((Text)controlList.get(1)).getText());
		/*
		controlList.get(0;)
		Map m = new Hashtable();

		for (Iterator i = controlList.iterator(); i.hasNext();) {
			Control t = (Control) i.next();
			String val = null;
			InstallParameter ip = (InstallParameter) t.getData();
			
			if (t instanceof Text) {
				val = ((Text)t).getText();
			} else {
				throw new RuntimeException("Unknown control type: " + t.getClass().getName());
			}
			
			m.put(ip.getLabel(), val);
		}
		model.put(OPTION_MAP, m);*/
	}

	@Override
	protected boolean validatePage() {
		return true;
	}

}
