/**
 * 
 */
package org.yocto.bc.ui.wizards.install;

class InstallParameter {
	public static final int DT_TEXT = 1;
	public static final int DT_COMBO = 2;
	public static final int DT_LIST = 3;
	public static final int DT_NUMBER = 4;
	public static final int DT_DIRECTORY = 5;
	public static final int DT_FILE = 6;

	private boolean valid = false;

	public int getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public boolean isRequired() {
		return required;
	}

	public String getData() {
		return data;
	}

	public String getHelpURL() {
		return helpURL;
	}

	int type;
	String label;
	private boolean required;
	private String data;
	private String helpURL;

	public InstallParameter(String var) {
		// {|Datatype|Label|UnRequired|Data|Help|}
		// {|T|Distribution|R|angstrom-2008.1|http://wiki.openembedded.net/index.php/Getting_started#Create_local_configuration|}

		String[] elems = var.split("\\|");

		if (elems.length == 5) {
			if (elems[0].equals("T")) {
				type = DT_TEXT;
			} else if (elems[0].equals("D")) {
				type = DT_DIRECTORY;
			} else if (elems[0].equals("F")) {
				type = DT_FILE;
			} else {
				// Unimplemented or unrecognized type
				return;
			}

			label = elems[1];

			if (elems[2].equals("R")) {
				required = true;
			} else if (elems[2].equals("U")) {
				required = false;
			} else {
				// Invalid required setting.
				return;
			}

			data = elems[3].trim();
			helpURL = elems[4].trim();

			valid = true;
		}
	}

	public boolean isValid() {

		return valid;
	}

}