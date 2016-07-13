//
// OrcPluginIds.java -- Java class OrcPluginIds
// Project OrcEclipse
//
// Created by jthywiss on Jul 12, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

/**
 * Constant identifier strings used in the plug-in manifest. There is a nested
 * interface for each ID type (i.e. every namespace/registry).
 * <p>
 * This is not a global strings table. For strings declared and used outside the
 * plug-in manifest, define them on an appropriate class or interface. This
 * interface exists because the plug-in manifest has no corresponding Java
 * location to declare these identifier strings.
 *
 * @author jthywiss
 */
@SuppressWarnings("nls")
public interface OrcPluginIds {
    /**
     * Get the plug-in ID, which is the OSGi Bundle-SymbolicName
     */
    static String pluginId = OrcPlugin.getId();

    /**
     * Prefixes an ID with our plug-in ID
     *
     * @param simpleId a "simple" ID: one with no dots
     * @return a full ID: plug-in ID + '.' + simple ID
     */
    static String makeId(final String simpleId) {
        return OrcPlugin.getId() + '.' + simpleId;
    }

    /**
     * Identifiers of user-defined extensions in the Orc plug-in's plug-in
     * manifest. These correspond to the <code>id</code> attribute of the
     * <code>extension</code> elements that are children of the root
     * <code>plugin</code> element in the <code>plugin.xml</code> file in the
     * Orc plug-in bundle.
     */
    @SuppressWarnings("javadoc")
    public interface Extension {
        static String CONTENT_TYPES = makeId("contentTypes");
        /* Keep this ID as-is for compatibility with old projects */
        static String ORC_BUILDER = makeId("build.orcBuilder");
        /* Keep this ID as-is for compatibility with old projects */
        static String ORC_NATURE = makeId("project.orcNature");
        /* Keep this ID as-is for compatibility with old projects */
        static String PROBLEM_MARKER = makeId("problemmarker");
        /* Keep this ID as-is for compatibility with old projects */
        static String PARSE_PROBLEM_MARKER = makeId("parse.problemmarker");
    }

    /**
     * Identifiers of file content types.
     *
     * @see org.eclipse.core.runtime.content.IContentType
     */
    @SuppressWarnings("javadoc")
    public interface ContentType {
        /*
         * No makeID calls for these, since they correspond to file types
         * independent of this plug-in
         */
        static String ORC_SOURCE_CODE = "edu.utexas.cs.orc.source";
        static String ORC_INCLUDE_FILE = "edu.utexas.cs.orc.include";
        static String OIL_FILE = "edu.utexas.cs.orc.oil";
    }

    /**
     * Identifiers of commands.
     *
     * @see org.eclipse.core.commands.Command
     */
    @SuppressWarnings("javadoc")
    public interface Command {
        static String ENABLE_ORC_NATURE = makeId("enableOrcNature");
        static String COMMENT = makeId("commentCommand");
        static String UNCOMMENT = makeId("uncommentCommand");
    }

    /**
     * Identifiers of command categories.
     *
     * @see org.eclipse.core.commands.Category
     */
    @SuppressWarnings("javadoc")
    public interface CommandCategory {
        static String ORC = makeId("orcCommandCategory");
    }

    /**
     * Identifiers of actions.
     * <p>
     * Actions are supposedly replaced by commands, but actions are unavoidable
     * in many parts of the API still. Note that an "action definition" is an
     * old name for a command.
     *
     * @see org.eclipse.jface.action.IAction
     */
    @SuppressWarnings("javadoc")
    public interface Action {
        static String COMMENT = makeId("CommentAction");
        static String UNCOMMENT = makeId("UncommentAction");
    }

    /**
     * Identifiers of contexts.
     *
     * @see org.eclipse.core.commands.contexts.Context
     */
    @SuppressWarnings("javadoc")
    public interface Context {
        static String ORC_EDITOR_SCOPE = makeId("orcEditorScope");
    }

    /**
     * Identifiers of "contribution items", which are menus, menu items, tool
     * bar items, etc.
     *
     * @see org.eclipse.jface.action.IContributionItem
     */
    @SuppressWarnings("javadoc")
    public interface ContributionItems {
        static String ORC_EDITOR_CONTEXT_MENU = OrcPluginIds.Editor.ORC_EDITOR + ".EditorContext";
        static String ORC_EDITOR_RULER_CONTEXT_MENU = OrcPluginIds.Editor.ORC_EDITOR + ".RulerContext";
        static String XXXXXX = makeId("xxxxxx");
    }

    /**
     * Identifiers of editors.
     *
     * @see org.eclipse.ui.IEditorDescriptor
     */
    @SuppressWarnings("javadoc")
    public interface Editor {
        static String ORC_EDITOR = makeId("orcEditor");
    }

    /**
     * Identifiers of project nature images.
     *
     * @see org.eclipse.core.resources.IProjectNature
     */
    @SuppressWarnings("javadoc")
    public interface ProjectNatureImage {
        static String ORC_PROJECT_NATURE_IMAGE = makeId("orcProjectNatureImage");
    }

    /**
     * Identifiers of property pages that appear in the "Properties" dialog for
     * resources etc.
     *
     * @see org.eclipse.ui.IWorkbenchPropertyPage
     */
    @SuppressWarnings("javadoc")
    public interface PropertyPage {
        static String ORC_PROJECT_SETTINGS = makeId("orcProjectPropertyPage");
    }

    /**
     * Identifiers of launch configuration types.
     *
     * @see org.eclipse.debug.core.ILaunchConfigurationType
     */
    @SuppressWarnings("javadoc")
    public interface LaunchConfigurationType {
        static String ORC_PROGRAM = makeId("orcProgramLaunch");
    }

    /**
     * Identifiers of console pattern match listeners.
     *
     * @see org.eclipse.ui.console.IPatternMatchListener
     */
    @SuppressWarnings("javadoc")
    public interface ConsolePatternMatchListener {
        static String HTTP = makeId("httpPatternMatchListener");
        static String ORC_POSITION = makeId("orcPosPatternMatchListener");
        static String ORC_EXCEPTION = makeId("orcExceptionPatternMatchListener");
    }

    /**
     * Identifiers of launch configuration tab groups.
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup
     */
    @SuppressWarnings("javadoc")
    public interface LaunchConfigurationTabGroup {
        static String ORC = makeId("orcLaunchConfigurationTabGroup");
    }

    /**
     * Identifiers of launch configuration tabs.
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab
     */
    @SuppressWarnings("javadoc")
    public interface LaunchConfigurationTab {
        static String ORC_GENERAL = makeId("orcGeneralTab");
        static String ORC_RUNTIME_CLASSPATH = makeId("orcRuntimeClasspathTab");
    }

    /**
     * Identifiers of launch shortcuts.
     *
     * @see org.eclipse.debug.ui.ILaunchShortcut
     */
    @SuppressWarnings("javadoc")
    public interface LaunchShortcut {
        static String RUN_ORC_PROGRAM = makeId("runOrcLaunchShortcut");
    }

    /**
     * Identifiers of images registered with the debug plug-in.
     */
    @SuppressWarnings("javadoc")
    public interface DebugPluginImage {
        static String ORC_PROGRAM_LAUNCH = makeId("orcProgramLaunchImage");
    }

    /**
     * Identifiers of perspectives.
     *
     * @see org.eclipse.ui.IPerspectiveFactory
     */
    @SuppressWarnings("javadoc")
    public interface Perspective {
        static String MAIN_ORC_PERSPECTIVE = makeId("mainOrcPerspective");
    }

    /**
     * Identifiers of creation wizards. These appear, for example, in the
     * File/New menu.
     *
     * @see org.eclipse.ui.INewWizard
     * @see org.eclipse.ui.wizards.IWizardDescriptor
     */
    @SuppressWarnings("javadoc")
    public interface NewWizard {
        static String NEW_ORC_PROJECT = makeId("newOrcProjectWizard");
        static String NEW_ORC_FILE = makeId("newOrcFileWizard");
    }

    /**
     * Identifiers of import wizard categories.
     *
     * @see org.eclipse.ui.wizards.IWizardCategory
     */
    @SuppressWarnings("javadoc")
    public interface NewWizardCategory {
        static String ORC = makeId("orcWizardCategory");
    }

}
