//
// OrcLaunchDelegate.java -- Java class OrcLaunchDelegate
// Project OrcEclipse
//
// $Id: OrcLaunchDelegate.java 1230 2009-08-18 14:58:16Z jthywissen $
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.ibm.icu.text.MessageFormat;

import edu.utexas.cs.orc.orceclipse.Activator;

/**
 * Launches an Orc program.
 * <p>
 * A launch configuration delegate performs launching for a
 * specific type of launch configuration. A launch configuration
 * delegate is defined by the <code>delegate</code> attribute
 * of a <code>launchConfigurationType</code> extension.
 *
 * @author jthywiss
 */
public class OrcLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {

	/**
	 * Launch configuration extension type ID for an Orc Program Launch configuration
	 */
	public static final String LAUNCH_CONFIG_ID = "edu.utexas.cs.orc.orceclipse.launch.orcApplication";

	public static final String TYPE_CHECK_ATTR_NAME = Activator.getInstance().getID() + ".TYPE_CHECK";
	public static final boolean TYPE_CHECK_ATTR_DEFAULT = false;
	public static final String NO_PRELUDE_ATTR_NAME = Activator.getInstance().getID() + ".NO_PRELUDE";
	public static final boolean NO_PRELUDE_ATTR_DEFAULT = false;
	// TODO: -I VAL : Set the include path for Orc includes (same syntax as
	// CLASSPATH). Default is ".", the current directory. Prelude files are
	// always available for include regardless of this setting.
	// TODO: -cp VAL : Set the class path for Orc sites (same syntax as
	// CLASSPATH). This is only used for classes not found in the Java VM
	// classpath.
	public static final String OIL_OUT_ATTR_NAME = Activator.getInstance().getID() + ".OIL_OUT";
	public static final String OIL_OUT_DEFAULT = "";
	public static final String MAX_PUBS_ATTR_NAME = Activator.getInstance().getID() + ".MAX_PUBS";
	public static final int MAX_PUBS_DEFAULT = 0;
	public static final String NUM_SITE_THREADS_ATTR_NAME = Activator.getInstance().getID() + ".NUM_SITE_THREADS";
	public static final int NUM_SITE_THREADS_DEFAULT = 2;
	public static final String TRACE_OUT_ATTR_NAME = Activator.getInstance().getID() + ".TRACE_OUT";
	public static final String TRACE_OUT_DEFAULT = "";
	public static final String DEBUG_LEVEL_ATTR_NAME = Activator.getInstance().getID() + ".DEBUG_LEVEL";
	public static final int DEBUG_LEVEL_DEFAULT = 0;

	/**
	 * @return LaunchConfigurationType for Orc Applications
	 */
	public static ILaunchConfigurationType getLaunchConfigType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(OrcLaunchDelegate.LAUNCH_CONFIG_ID);
	}

	/**
	 * @param configuration LaunchConfigurationWorkingCopy to update
	 */
	public static void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
		//configuration.setAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", "OrcJava");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {

		// Derived from org.eclipse.jdt.launching.JavaLaunchDelegate.java,
		// Revision 1.8 (02 Oct 2007), trunk rev as of 04 Aug 2009

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(MessageFormat.format("{0}...", new String[] { configuration.getName() }), 2); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		try {
			monitor.subTask("Verifying launch attributes...");

			final String mainTypeName = "orc.Orc";
			final IVMRunner runner = getVMRunner(configuration, mode);

			final File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}

			// Environment variables
			final String[] envp = getEnvironment(configuration);

			// Program & VM arguments
			String pgmArgs = "";
			if (configuration.getAttribute(TYPE_CHECK_ATTR_NAME, TYPE_CHECK_ATTR_DEFAULT)) {
				pgmArgs += "-typecheck ";
			}
			if (configuration.getAttribute(NO_PRELUDE_ATTR_NAME, NO_PRELUDE_ATTR_DEFAULT)) {
				pgmArgs += "-noprelude ";
			}
			// TODO: -I VAL : Set the include path for Orc includes (same syntax
			// as CLASSPATH). Default is ".", the current directory. Prelude
			// files are always available for include regardless of this
			// setting.
			// TODO: -cp VAL : Set the class path for Orc sites (same syntax as
			// CLASSPATH). This is only used for classes not found in the Java
			// VM classpath.
			if (configuration.getAttribute(OIL_OUT_ATTR_NAME, "").length() > 0) {
				pgmArgs += "-oilOut \'" + configuration.getAttribute(OIL_OUT_ATTR_NAME, "") + "\' ";
			}
			if (configuration.getAttribute(MAX_PUBS_ATTR_NAME, -1) != -1) {
				pgmArgs += "-pub " + configuration.getAttribute(MAX_PUBS_ATTR_NAME, 0) + " ";
			}
			if (configuration.getAttribute(NUM_SITE_THREADS_ATTR_NAME, -1) != -1) {
				pgmArgs += "-numSiteThreads " + configuration.getAttribute(NUM_SITE_THREADS_ATTR_NAME, 0) + " ";
			}
			if (configuration.getAttribute(TRACE_OUT_ATTR_NAME, "").length() > 0) {
				pgmArgs += "-trace \'" + configuration.getAttribute(TRACE_OUT_ATTR_NAME, "") + "\' ";
			}
			for (int i = 0; i < configuration.getAttribute(DEBUG_LEVEL_ATTR_NAME, 0); i++) {
				pgmArgs += "-debug ";
			}
			pgmArgs += "-- ${resource_loc}";
			pgmArgs = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(pgmArgs);
			final String vmArgs = getVMArguments(configuration);
			final ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);

			// VM-specific attributes
			final Map vmAttributesMap = getVMSpecificAttributesMap(configuration);

			// Classpath
			//final String[] classpath = getClasspath(configuration);
			//FIXME: Compute path & jar dynamically, or at least use Eclipse variables!
			final String[] classpath = { "/Users/jthywiss/Projects/Eclipse_workspace/OrcEclipse/lib/orc-0.9.9.jar" };

			// Create VM config
			final VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(execArgs.getVMArgumentsArray());
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);

			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			// done the verification phase
			monitor.worked(1);

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			monitor.done();
		}
	}

}
