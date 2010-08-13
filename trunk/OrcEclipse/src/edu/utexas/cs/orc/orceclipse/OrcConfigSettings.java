//
// OrcConfigSettings.java -- Java class OrcConfigSettings
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Sep 3, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.io.IOException;
import java.util.Arrays;

import orc.Main.OrcCmdLineOptions$1;
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
public class OrcConfigSettings extends OrcCmdLineOptions$1 {
	public static final String TYPE_CHECK_ATTR_NAME = Activator.getInstance().getID() + ".TYPE_CHECK"; //$NON-NLS-1$
	public static final String NO_PRELUDE_ATTR_NAME = Activator.getInstance().getID() + ".NO_PRELUDE"; //$NON-NLS-1$
	public static final String EXCEPTIONS_ON_ATTR_NAME = Activator.getInstance().getID() + ".EXCEPTIONS_ON"; //$NON-NLS-1$
	public static final String INCLUDE_PATH_ATTR_NAME = Activator.getInstance().getID() + ".INCLUDE_PATH"; //$NON-NLS-1$
	public static final String SITE_CLASSPATH_ATTR_NAME = Activator.getInstance().getID() + ".SITE_CLASSPATH"; //$NON-NLS-1$
	public static final String OIL_OUT_ATTR_NAME = Activator.getInstance().getID() + ".OIL_OUT"; //$NON-NLS-1$
	public static final String MAX_PUBS_ATTR_NAME = Activator.getInstance().getID() + ".MAX_PUBS"; //$NON-NLS-1$
	public static final String NUM_SITE_THREADS_ATTR_NAME = Activator.getInstance().getID() + ".NUM_SITE_THREADS"; //$NON-NLS-1$
	public static final String TRACE_OUT_ATTR_NAME = Activator.getInstance().getID() + ".TRACE_OUT"; //$NON-NLS-1$
	public static final String DEBUG_LEVEL_ATTR_NAME = Activator.getInstance().getID() + ".DEBUG_LEVEL"; //$NON-NLS-1$

	private static final OrcBindings defaultConfig = new OrcBindings();

	public static final boolean TYPE_CHECK_DEFAULT = defaultConfig.typecheck();
	public static final boolean NO_PRELUDE_DEFAULT = !defaultConfig.usePrelude();
	public static final boolean EXCEPTIONS_ON_DEFAULT = defaultConfig.exceptionsOn();
	public static final String INCLUDE_PATH_DEFAULT = defaultConfig.includePath().isEmpty() ? "" : listMkString(defaultConfig.includePath(), ":").concat(":"); //Eclipse path pref entries always have a trailing : //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public static final String SITE_CLASSPATH_DEFAULT = defaultConfig.classPath().isEmpty() ? "" : listMkString(defaultConfig.classPath(), ":").concat(":"); //Eclipse path pref entries always have a trailing : //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	//public static final String OIL_OUT_DEFAULT = defaultConfig.oilOutputFile().getPath();
	public static final int MAX_PUBS_DEFAULT = defaultConfig.maxPublications();
	//public static final int NUM_SITE_THREADS_DEFAULT = defaultConfig.numSiteThreads();
	//public static final String TRACE_OUT_DEFAULT = defaultConfig.traceOutputFile().getPath();
	public static final int DEBUG_LEVEL_DEFAULT = defaultConfig.debugLevel();

	/**
	 * Constructs an object of class OrcConfigSettings.
	 *
	 * @param project
	 * @param launchConfig
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public OrcConfigSettings(final IProject project, final ILaunchConfiguration launchConfig) throws IOException, CoreException {
		super();
		if (project != null) {
			fillFromProject(project);
		}
		if (launchConfig != null) {
			fillFromLaunchConfig(launchConfig);
		}
	}

	/**
	 * @param project
	 * @throws IOException 
	 */
	private void fillFromProject(final IProject project) throws IOException {
		final PreferencesService prefSvc = Activator.getInstance().getPreferencesService();
		prefSvc.setProject(project);

		// Will also look upwards in prefs levels if not found in project.

		if (prefSvc.isDefined(TYPE_CHECK_ATTR_NAME)) {
			typecheck_$eq(prefSvc.getBooleanPreference(TYPE_CHECK_ATTR_NAME));
		}
		if (prefSvc.isDefined(NO_PRELUDE_ATTR_NAME)) {
			usePrelude_$eq(!prefSvc.getBooleanPreference(NO_PRELUDE_ATTR_NAME));
		}
		if (prefSvc.isDefined(EXCEPTIONS_ON_ATTR_NAME)) {
			exceptionsOn_$eq(prefSvc.getBooleanPreference(EXCEPTIONS_ON_ATTR_NAME));
		}
		if (prefSvc.isDefined(INCLUDE_PATH_ATTR_NAME)) {
			includePath_$eq(Arrays.asList(prefSvc.getStringPreference(INCLUDE_PATH_ATTR_NAME).split(":"))); //$NON-NLS-1$
		}
		if (prefSvc.isDefined(SITE_CLASSPATH_ATTR_NAME)) {
			classPath_$eq(Arrays.asList(prefSvc.getStringPreference(SITE_CLASSPATH_ATTR_NAME).split(":"))); //$NON-NLS-1$
		}
		//if (prefSvc.isDefined(OIL_OUT_ATTR_NAME)) {
		//	oilOutputFile_$eq(new File(prefSvc.getStringPreference(OIL_OUT_ATTR_NAME)));
		//}
		if (prefSvc.isDefined(MAX_PUBS_ATTR_NAME)) {
			maxPublications_$eq(prefSvc.getIntPreference(MAX_PUBS_ATTR_NAME));
		}
		//if (prefSvc.isDefined(NUM_SITE_THREADS_ATTR_NAME)) {
		//	numSiteThreads_$eq(prefSvc.getIntPreference(NUM_SITE_THREADS_ATTR_NAME));
		//}
		//if (prefSvc.isDefined(TRACE_OUT_ATTR_NAME)) {
		//	try {
		//		traceOutputFile_$eq(new File(prefSvc.getStringPreference(TRACE_OUT_ATTR_NAME)));
		//	} catch (final CmdLineException e) {
		//		throw new IOException(e.getMessage());
		//	}
		//}
		if (prefSvc.isDefined(DEBUG_LEVEL_ATTR_NAME)) {
			debugLevel_$eq(prefSvc.getIntPreference(DEBUG_LEVEL_ATTR_NAME));
		}
	}

	/**
	 * @param launchConfig
	 * @throws CoreException 
	 * @throws IOException 
	 */
	private void fillFromLaunchConfig(final ILaunchConfiguration launchConfig) throws CoreException, IOException {

		typecheck_$eq(launchConfig.getAttribute(TYPE_CHECK_ATTR_NAME, typecheck()));
		usePrelude_$eq(!launchConfig.getAttribute(NO_PRELUDE_ATTR_NAME, !usePrelude()));
		exceptionsOn_$eq(launchConfig.getAttribute(EXCEPTIONS_ON_ATTR_NAME, exceptionsOn()));
		includePath_$eq(launchConfig.getAttribute(INCLUDE_PATH_ATTR_NAME, includePath()));
		classPath_$eq(launchConfig.getAttribute(SITE_CLASSPATH_ATTR_NAME, classPath()));
		//if (launchConfig.getAttribute(OIL_OUT_ATTR_NAME, (String) null) != null) {
		//	oilOutputFile_$eq(new File(launchConfig.getAttribute(OIL_OUT_ATTR_NAME, (String) null)));
		//}
		maxPublications_$eq(launchConfig.getAttribute(MAX_PUBS_ATTR_NAME, maxPublications()));
		//numSiteThreads_$eq(launchConfig.getAttribute(NUM_SITE_THREADS_ATTR_NAME, numSiteThreads()));
		//if (launchConfig.getAttribute(TRACE_OUT_ATTR_NAME, (String) null) != null) {
		//	try {
		//		traceOutputFile_$eq(new File(launchConfig.getAttribute(TRACE_OUT_ATTR_NAME, (String) null)));
		//	} catch (final CmdLineException e) {
		//		throw new IOException(e.getMessage());
		//	}
		//}
		debugLevel_$eq(launchConfig.getAttribute(DEBUG_LEVEL_ATTR_NAME, debugLevel()));
	}

	protected static void initDefaultPrefs() {
		// We don't want to use a preferences.ini / preferences.properties file for default preferences,
		// but instead get them from the OrcOptions class's defaults. Activator gives us the opportunity to set the defaults here.
		final IEclipsePreferences defaultPrefs = new DefaultScope().getNode(Activator.getInstance().getLanguageID());
		defaultPrefs.putBoolean(TYPE_CHECK_ATTR_NAME, TYPE_CHECK_DEFAULT);
		defaultPrefs.putBoolean(NO_PRELUDE_ATTR_NAME, NO_PRELUDE_DEFAULT);
		defaultPrefs.putBoolean(EXCEPTIONS_ON_ATTR_NAME, EXCEPTIONS_ON_DEFAULT);
		defaultPrefs.put(INCLUDE_PATH_ATTR_NAME, INCLUDE_PATH_DEFAULT);
		defaultPrefs.put(SITE_CLASSPATH_ATTR_NAME, SITE_CLASSPATH_DEFAULT);
		//defaultPrefs.put(OIL_OUT_ATTR_NAME, OIL_OUT_DEFAULT);
		defaultPrefs.putInt(MAX_PUBS_ATTR_NAME, MAX_PUBS_DEFAULT);
		//defaultPrefs.putInt(NUM_SITE_THREADS_ATTR_NAME, NUM_SITE_THREADS_DEFAULT);
		//defaultPrefs.put(TRACE_OUT_ATTR_NAME, TRACE_OUT_DEFAULT);
		defaultPrefs.putInt(DEBUG_LEVEL_ATTR_NAME, DEBUG_LEVEL_DEFAULT);
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
}
