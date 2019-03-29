//
// Messages.java -- Java class Messages
// Project OrcEclipse
//
// Created by jthywiss on Sep 8, 2009.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
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
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "edu.utexas.cs.orc.orceclipse.messages"; //$NON-NLS-1$

    public static String Activator_InternalError;
    public static String EnableOrcNature_AlreadJavaErrorMessage;
    public static String EnableOrcNature_AlreadyJavaErrorTitle;
    public static String NewOrcFileWizardPage_Descrption;
    public static String NewOrcFileWizardPage_Title;
    public static String NewOrcIncludeFileWizardPage_Descrption;
    public static String NewOrcIncludeFileWizardPage_Title;
    public static String OrcBuilder_BuildingOrcFile;
    public static String OrcBuilder_BuildingProject;
    public static String OrcBuilder_CleaningProject;
    public static String OrcBuilder_CompilerInternalErrorOn;
    public static String OrcBuilder_Compiling;
    public static String OrcBuilder_Done;
    public static String OrcBuilder_DoneBuildingOrcFile;
    public static String OrcBuilder_IOErrorWhenBuilding;
    public static String OrcBuilder_OrcCompilerConsoleName;
    public static String OrcBuilder_Preparing;
    public static String OrcGeneralLaunchConfigurationTab_DumpStackLabel;
    public static String OrcGeneralLaunchConfigurationTab_EchoOilLabel;
    public static String OrcGeneralLaunchConfigurationTab_GeneralTabName;
    public static String OrcGeneralLaunchConfigurationTab_LogLevelLabel;
    public static String OrcGeneralLaunchConfigurationTab_MaxSiteThreadsLabel;
    public static String OrcGeneralLaunchConfigurationTab_NoTcoLabel;
    public static String OrcGeneralLaunchConfigurationTab_runtimeVersion;
    public static String OrcGeneralLaunchConfigurationTab_StackSizeLabel;
    public static String OrcGeneralLaunchConfigurationTab_TokenLimitLabel;
    public static String OrcLaunchDelegate_UnableToLaunchNoResourceSelected;
    public static String OrcLaunchDelegate_VerifyingLaunchAttributes;
    public static String OrcLaunchShortcut_OrcProgramLaunchConfigName;
    public static String OrcLaunchShortcut_SelectLaunchConfigMessage;
    public static String OrcLaunchShortcut_SelectLaunchConfigTitle;
    public static String OrcLaunchShortcut_UnableToLaunchMessage;
    public static String OrcLaunchShortcut_UnableToLaunchTitle;
    public static String OrcPathEditor_AddFolderMessage1;
    public static String OrcPathEditor_AddFolderMessage2;
    public static String OrcPathEditor_AddFolderTitle;
    public static String OrcPathEditor_AddJarFileMessage1;
    public static String OrcPathEditor_AddJarFileMessage2;
    public static String OrcPathEditor_ChooseFolder;
    public static String OrcPathEditor_ExternalFolder;
    public static String OrcPathEditor_ExternalJarFile;
    public static String OrcPathEditor_Folder;
    public static String OrcPathEditor_JarFile;
    public static String OrcPathEditor_JarFileDialogTitle;
    public static String OrcPathEditor_TypeDialogMessage1;
    public static String OrcPathEditor_TypeDialogMessage2;
    public static String OrcPathEditor_TypeDialogTitle;
    public static String OrcProjectPropertyPage_IncludePathDescription;
    public static String OrcProjectPropertyPage_IncludePathLabel;
    public static String OrcProjectPropertyPage_RecursionCheckLabel;
    public static String OrcProjectPropertyPage_UseStdPreludeLabel;
    public static String OrcProjectPropertyPage_SiteClassPathLabel;
    public static String OrcProjectPropertyPage_SitePathDescription;
    public static String OrcProjectPropertyPage_TypeCheckLabel;
    public static String OrcRuntimeClasspathTab_RuntimeClasspathTabName;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        /* Nothing to do */
    }

}
