//
// Messages.java -- Java class Messages
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Sep 8, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import org.eclipse.osgi.util.NLS;

/**
 * Eclipse message bundle class for the edu.utexas.cs.orc.orceclipse package.
 *
 * @author jthywiss
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "edu.utexas.cs.orc.orceclipse.messages"; //$NON-NLS-1$
	public static String Activator_InternalError;
	public static String NewOrcFileWizardPage_Descrption;
	public static String NewOrcFileWizardPage_Title;
	public static String OrcBuilder_BuildingOrcFile;
	public static String OrcBuilder_CompilerInternalErrorOn;
	public static String OrcBuilder_DoneBuildingOrcFile;
	public static String OrcBuilder_IOErrorWhenBuilding;
	public static String OrcBuilder_OrcCompilerConsoleName;
	public static String OrcGeneralLaunchConfigurationTab_DebugLevelLabel;
	public static String OrcGeneralLaunchConfigurationTab_GeneralTabName;
	public static String OrcGeneralLaunchConfigurationTab_MaxPubsLabel;
	public static String OrcGeneralLaunchConfigurationTab_NumSiteThreadsLabel;
	public static String OrcGeneralLaunchConfigurationTab_TraceOutFilenameLabel;
	public static String OrcLaunchDelegate_UnableToLaunchNoResourceSelected;
	public static String OrcLaunchDelegate_VerifyingLaunchAttributes;
	public static String OrcLaunchShortcut_OrcProgramLaunchConfigName;
	public static String OrcLaunchShortcut_SelectLaunchConfigMessage;
	public static String OrcLaunchShortcut_SelectLaunchConfigTitle;
	public static String OrcLaunchShortcut_UnableToLaunchMessage;
	public static String OrcLaunchShortcut_UnableToLaunchTitle;
	public static String OrcProjectPropertyPage_EnableExceptionsLabel;
	public static String OrcProjectPropertyPage_IncludePathLabel;
	public static String OrcProjectPropertyPage_IncludePathMessage;
	public static String OrcProjectPropertyPage_NoStdPreludeLabel;
	public static String OrcProjectPropertyPage_SiteClassPathLabel;
	public static String OrcProjectPropertyPage_SiteClassPathMessage;
	public static String OrcProjectPropertyPage_TypeCheckLabel;
	public static String OrcRuntimeClasspathTab_RuntimeClasspathTabName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
