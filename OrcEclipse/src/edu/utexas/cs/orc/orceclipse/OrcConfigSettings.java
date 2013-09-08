//
// OrcConfigSettings.java -- Java class OrcConfigSettings
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Sep 3, 2009.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import orc.Main.OrcCmdLineOptions;
import orc.script.OrcBindings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.imp.preferences.PreferencesService;

/**
 * Orc configuration ({@link orc.OrcOptions}) extended to read its state from
 * Eclipse preferences and run configurations.
 * <p>
 * Note that OrcConfigSettings uses Eclipse's default preferences hierarchy,
 * as wrapped by IMP's {@link org.eclipse.imp.preferences.PreferencesService},
 * AND extends that by treating the launch configuration (if supplied) as a new
 * lowest level preference.  So, the resulting hierarchy is:
 * LAUNCH_CONFIGURATION -> PROJECT -> INSTANCE -> CONFIGURATION -> DEFAULT
 *
 * @see org.eclipse.imp.preferences.PreferencesService
 * @see org.eclipse.debug.core.ILaunchConfiguration
 * @author jthywiss
 */
public class OrcConfigSettings extends OrcCmdLineOptions {
	public static final String PATH_SEPARATOR = "|"; //$NON-NLS-1$

	public static final String LOG_LEVEL_ATTR_NAME = Activator.getInstance().getID() + ".LOG_LEVEL"; //$NON-NLS-1$
	public static final String PRELUDE_ATTR_NAME = Activator.getInstance().getID() + ".USE_PRELUDE"; //$NON-NLS-1$
	public static final String INCLUDE_PATH_ATTR_NAME = Activator.getInstance().getID() + ".INCLUDE_PATH"; //$NON-NLS-1$
	public static final String ADDITIONAL_INCLUDES_ATTR_NAME = Activator.getInstance().getID() + ".ADDITIONAL_INCLUDES"; //$NON-NLS-1$
	public static final String TYPE_CHECK_ATTR_NAME = Activator.getInstance().getID() + ".TYPE_CHECK"; //$NON-NLS-1$
	public static final String RECURSION_CHECK_ATTR_NAME = Activator.getInstance().getID() + ".RECURSION_CHECK"; //$NON-NLS-1$
	public static final String ECHO_OIL_ATTR_NAME = Activator.getInstance().getID() + ".ECHO_OIL"; //$NON-NLS-1$
	//public static final String OIL_OUT_ATTR_NAME = Activator.getInstance().getID() + ".OIL_OUT"; //$NON-NLS-1$
	public static final String SITE_CLASSPATH_ATTR_NAME = Activator.getInstance().getID() + ".SITE_CLASSPATH"; //$NON-NLS-1$
	public static final String SHOW_JAVA_STACK_TRACE_ATTR_NAME = Activator.getInstance().getID() + ".SHOW_JAVA_STACK_TRACE"; //$NON-NLS-1$
	public static final String NO_TCO_ATTR_NAME = Activator.getInstance().getID() + ".NO_TCO"; //$NON-NLS-1$
	public static final String MAX_STACK_DEPTH_ATTR_NAME = Activator.getInstance().getID() + ".MAX_STACK_DEPTH"; //$NON-NLS-1$
	public static final String MAX_TOKENS_ATTR_NAME = Activator.getInstance().getID() + ".MAX_TOKENS"; //$NON-NLS-1$
	public static final String MAX_SITE_THREADS_ATTR_NAME = Activator.getInstance().getID() + ".MAX_SITE_THREADS"; //$NON-NLS-1$

	private static final OrcBindings defaultConfig = new OrcBindings();

	public static final String LOG_LEVEL_DEFAULT = defaultConfig.logLevel();
	public static final boolean PRELUDE_DEFAULT = defaultConfig.usePrelude();
	public static final String INCLUDE_PATH_DEFAULT = defaultConfig.includePath().isEmpty() ? "" : listMkString(defaultConfig.includePath(), PATH_SEPARATOR).concat(PATH_SEPARATOR); //Eclipse path pref entries always have a trailing : //$NON-NLS-1$
	public static final String ADDITIONAL_INCLUDES_DEFAULT = defaultConfig.additionalIncludes().isEmpty() ? "" : listMkString(defaultConfig.additionalIncludes(), PATH_SEPARATOR).concat(PATH_SEPARATOR); //Eclipse path pref entries always have a trailing : //$NON-NLS-1$
	public static final boolean TYPE_CHECK_DEFAULT = defaultConfig.typecheck();
	public static final boolean RECURSION_CHECK_DEFAULT = !defaultConfig.disableRecursionCheck();
	public static final boolean ECHO_OIL_DEFAULT = defaultConfig.echoOil();
	//public static final String OIL_OUT_DEFAULT = defaultConfig.oilOutputFile().getPath();
	public static final String SITE_CLASSPATH_DEFAULT = defaultConfig.classPath().isEmpty() ? "" : listMkString(defaultConfig.classPath(), PATH_SEPARATOR).concat(PATH_SEPARATOR); //Eclipse path pref entries always have a trailing : //$NON-NLS-1$
	public static final boolean SHOW_JAVA_STACK_TRACE_DEFAULT = defaultConfig.showJavaStackTrace();
	public static final boolean NO_TCO_DEFAULT = defaultConfig.disableTailCallOpt();
	public static final int MAX_STACK_DEPTH_DEFAULT = defaultConfig.stackSize();
	public static final int MAX_TOKENS_DEFAULT = defaultConfig.maxTokens();
	public static final int MAX_SITE_THREADS_DEFAULT = defaultConfig.maxSiteThreads();

	/**
	 * Constructs an object of class OrcConfigSettings.
	 *
	 * @param project Project (possibly null) to read Orc settings from
	 * @param launchConfig LaunchConfiguration (possibly null) to read Orc settings from
	 * @throws CoreException For problems fetching settings from the project or launch config.
	 */
	public OrcConfigSettings(final IProject project, final ILaunchConfiguration launchConfig) throws CoreException {
		super();
		if (project != null) {
			fillFromProject(project);
		}
		if (launchConfig != null) {
			fillFromLaunchConfig(launchConfig);
		}
	}

	private void fillFromProject(final IProject project) {
		final PreferencesService prefSvc = Activator.getInstance().getPreferencesService();
		prefSvc.setProject(project);

		// Will also look upwards in prefs levels if not found in project.

		if (prefSvc.isDefined(LOG_LEVEL_ATTR_NAME)) {
			logLevel_$eq(prefSvc.getStringPreference(LOG_LEVEL_ATTR_NAME));
		}
		if (prefSvc.isDefined(PRELUDE_ATTR_NAME)) {
			usePrelude_$eq(prefSvc.getBooleanPreference(PRELUDE_ATTR_NAME));
		}
		if (prefSvc.isDefined(INCLUDE_PATH_ATTR_NAME)) {
			includePath_$eq(stringToPathList(prefSvc.getStringPreference(INCLUDE_PATH_ATTR_NAME)));
		}
		if (prefSvc.isDefined(ADDITIONAL_INCLUDES_ATTR_NAME)) {
			additionalIncludes_$eq(stringToPathList(prefSvc.getStringPreference(ADDITIONAL_INCLUDES_ATTR_NAME)));
		}
		if (prefSvc.isDefined(TYPE_CHECK_ATTR_NAME)) {
			typecheck_$eq(prefSvc.getBooleanPreference(TYPE_CHECK_ATTR_NAME));
		}
		if (prefSvc.isDefined(RECURSION_CHECK_ATTR_NAME)) {
			disableRecursionCheck_$eq(!prefSvc.getBooleanPreference(RECURSION_CHECK_ATTR_NAME));
		}
		if (prefSvc.isDefined(ECHO_OIL_ATTR_NAME)) {
			echoOil_$eq(prefSvc.getBooleanPreference(ECHO_OIL_ATTR_NAME));
		}
		//if (prefSvc.isDefined(OIL_OUT_ATTR_NAME)) {
		//	oilOutputFile_$eq(new File(prefSvc.getStringPreference(OIL_OUT_ATTR_NAME)));
		//}
		if (prefSvc.isDefined(SITE_CLASSPATH_ATTR_NAME)) {
			classPath_$eq(stringToPathList(prefSvc.getStringPreference(SITE_CLASSPATH_ATTR_NAME)));
		}
		if (prefSvc.isDefined(SHOW_JAVA_STACK_TRACE_ATTR_NAME)) {
			showJavaStackTrace_$eq(prefSvc.getBooleanPreference(SHOW_JAVA_STACK_TRACE_ATTR_NAME));
		}
		if (prefSvc.isDefined(NO_TCO_ATTR_NAME)) {
			disableTailCallOpt_$eq(prefSvc.getBooleanPreference(NO_TCO_ATTR_NAME));
		}
		if (prefSvc.isDefined(MAX_STACK_DEPTH_ATTR_NAME)) {
			stackSize_$eq(prefSvc.getIntPreference(MAX_STACK_DEPTH_ATTR_NAME));
		}
		if (prefSvc.isDefined(MAX_TOKENS_ATTR_NAME)) {
			maxTokens_$eq(prefSvc.getIntPreference(MAX_TOKENS_ATTR_NAME));
		}
		if (prefSvc.isDefined(MAX_SITE_THREADS_ATTR_NAME)) {
			maxSiteThreads_$eq(prefSvc.getIntPreference(MAX_SITE_THREADS_ATTR_NAME));
		}
	}

	/**
	 * @param launchConfig
	 * @throws CoreException
	 */
	@SuppressWarnings("unchecked")
	private void fillFromLaunchConfig(final ILaunchConfiguration launchConfig) throws CoreException {

		logLevel_$eq(launchConfig.getAttribute(LOG_LEVEL_ATTR_NAME, logLevel()));
		usePrelude_$eq(launchConfig.getAttribute(PRELUDE_ATTR_NAME, usePrelude()));
		includePath_$eq(launchConfig.getAttribute(INCLUDE_PATH_ATTR_NAME, includePath()));
		additionalIncludes_$eq(launchConfig.getAttribute(ADDITIONAL_INCLUDES_ATTR_NAME, additionalIncludes()));
		typecheck_$eq(launchConfig.getAttribute(TYPE_CHECK_ATTR_NAME, typecheck()));
		disableRecursionCheck_$eq(!launchConfig.getAttribute(RECURSION_CHECK_ATTR_NAME, !disableRecursionCheck()));
		echoOil_$eq(launchConfig.getAttribute(ECHO_OIL_ATTR_NAME, echoOil()));
		//if (launchConfig.getAttribute(OIL_OUT_ATTR_NAME, (String) null) != null) {
		//	oilOutputFile_$eq(new File(launchConfig.getAttribute(OIL_OUT_ATTR_NAME, (String) null)));
		//}
		classPath_$eq(launchConfig.getAttribute(SITE_CLASSPATH_ATTR_NAME, classPath()));
		showJavaStackTrace_$eq(launchConfig.getAttribute(SHOW_JAVA_STACK_TRACE_ATTR_NAME, showJavaStackTrace()));
		disableTailCallOpt_$eq(launchConfig.getAttribute(NO_TCO_ATTR_NAME, disableTailCallOpt()));
		stackSize_$eq(launchConfig.getAttribute(MAX_STACK_DEPTH_ATTR_NAME, stackSize()));
		maxTokens_$eq(launchConfig.getAttribute(MAX_TOKENS_ATTR_NAME, maxTokens()));
		maxSiteThreads_$eq(launchConfig.getAttribute(MAX_SITE_THREADS_ATTR_NAME, maxSiteThreads()));
	}

	protected static void initDefaultPrefs() {
		// We don't want to use a preferences.ini / preferences.properties file for default preferences,
		// but instead get them from the OrcOptions class's defaults. Activator gives us the opportunity to set the defaults here.
		final IEclipsePreferences defaultPrefs = DefaultScope.INSTANCE.getNode(Activator.getInstance().getLanguageID());
		defaultPrefs.put(LOG_LEVEL_ATTR_NAME, LOG_LEVEL_DEFAULT);
		defaultPrefs.putBoolean(PRELUDE_ATTR_NAME, PRELUDE_DEFAULT);
		defaultPrefs.put(INCLUDE_PATH_ATTR_NAME, INCLUDE_PATH_DEFAULT);
		defaultPrefs.put(ADDITIONAL_INCLUDES_ATTR_NAME, ADDITIONAL_INCLUDES_DEFAULT);
		defaultPrefs.putBoolean(TYPE_CHECK_ATTR_NAME, TYPE_CHECK_DEFAULT);
		defaultPrefs.putBoolean(RECURSION_CHECK_ATTR_NAME, RECURSION_CHECK_DEFAULT);
		defaultPrefs.putBoolean(ECHO_OIL_ATTR_NAME, ECHO_OIL_DEFAULT);
		//defaultPrefs.put(OIL_OUT_ATTR_NAME, OIL_OUT_DEFAULT);
		defaultPrefs.put(SITE_CLASSPATH_ATTR_NAME, SITE_CLASSPATH_DEFAULT);
		defaultPrefs.putBoolean(SHOW_JAVA_STACK_TRACE_ATTR_NAME, SHOW_JAVA_STACK_TRACE_DEFAULT);
		defaultPrefs.putBoolean(NO_TCO_ATTR_NAME, NO_TCO_DEFAULT);
		defaultPrefs.putInt(MAX_STACK_DEPTH_ATTR_NAME, MAX_STACK_DEPTH_DEFAULT);
		defaultPrefs.putInt(MAX_TOKENS_ATTR_NAME, MAX_TOKENS_DEFAULT);
		defaultPrefs.putInt(MAX_SITE_THREADS_ATTR_NAME, MAX_SITE_THREADS_DEFAULT);
		//No need to flush() nodes in default scope
	}

	private static String listMkString(final Iterable<?> theList, final String sep) {
		final StringBuilder sb = new StringBuilder();
		for (final Object o : theList) {
			sb.append(o.toString());
			sb.append(sep);
		}
		return sb.substring(0, sb.length() - sep.length());
	}

	public static List<String> stringToPathList(final String pathsString) {
		if (pathsString.isEmpty()) {
			return new ArrayList<String>(0);
		}
		StringTokenizer st = new StringTokenizer(pathsString, pathsString.substring(pathsString.length()-1, pathsString.length()));
		ArrayList<String> pathList = new ArrayList<String>();
		while (st.hasMoreElements()) {
			pathList.add(st.nextToken());
		}
		return pathList;
	}

	public static String pathListToString(final List<String> pathList) {
		StringBuffer pathListString = new StringBuffer(""); //$NON-NLS-1$
		for (String path : pathList) {
			pathListString.append(path);
			pathListString.append(OrcConfigSettings.PATH_SEPARATOR);
		}
		return pathListString.toString();
	}
}
