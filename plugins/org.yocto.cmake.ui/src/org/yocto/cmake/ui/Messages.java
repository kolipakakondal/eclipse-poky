/*******************************************************************************
 * Copyright (c) 2018 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Intel Corporation - initial API and implementation
 *******************************************************************************/
package org.yocto.cmake.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.yocto.cmake.ui.messages"; //$NON-NLS-1$
	public static String CMakeConsole_Name;
	public static String CMakeImportWizard_0;
	public static String CMakeImportWizardPage_NoToolchain;
	public static String CMakeImportWizardPage_ShowSupportedToolchainOnly;
	public static String CMakeImportWizardPage_ToolchainForIndexer;
	public static String RegenerateHandler_ConfirmRegenerate;
	public static String RegenerateHandler_ConfirmRegenerationDialogTitle;
	public static String RegenerateHandler_DeleteBuildDirFailed;
	public static String RegenerateHandler_DeletingBuildDir;
	public static String RegenerateHandler_RefreshBuildDirFailed;
	public static String RegenerateHandler_RefreshingBuildDir;
	public static String RegenerateHandler_RegenerateBuildDirFailed;
	public static String RegenerateHandler_RegeneratingBuildDir;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
