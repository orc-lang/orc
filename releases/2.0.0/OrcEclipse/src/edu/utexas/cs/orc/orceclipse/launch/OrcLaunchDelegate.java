//
// OrcLaunchDelegate.java -- Java class OrcLaunchDelegate
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import orc.Main;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleException;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

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
@SuppressWarnings("restriction")
public class OrcLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {

	/**
	 * Launch configuration extension type ID for an Orc Program Launch configuration
	 */
	public static final String LAUNCH_CONFIG_ID = "edu.utexas.cs.orc.orceclipse.launch.orcApplication"; //$NON-NLS-1$

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
		// Currently, a minimal Orc launch config. is an empty one (no required attributes)
		// So, do nothing here
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, final IProgressMonitor monitor) throws CoreException {

		if (SelectedResourceManager.getDefault().getSelectedResource() == null) {
			StatusManager.getManager().handle(new Status(IStatus.INFO, Activator.getInstance().getID(), 1, Messages.OrcLaunchDelegate_UnableToLaunchNoResourceSelected, null), StatusManager.SHOW);
			return;
		}

		// Derived from org.eclipse.jdt.launching.JavaLaunchDelegate.java,
		// Revision 1.8 (02 Oct 2007), trunk rev as of 04 Aug 2009

		IProgressMonitor monitorNN = monitor;
		if (monitorNN == null) {
			monitorNN = new NullProgressMonitor();
		}
		monitorNN.beginTask(MessageFormat.format("{0}...", new String[] { configuration.getName() }), 2); //$NON-NLS-1$
		// check for cancellation
		if (monitorNN.isCanceled()) {
			return;
		}
		try {
			monitorNN.subTask(Messages.OrcLaunchDelegate_VerifyingLaunchAttributes);

			final IResource orcProgToLaunch = SelectedResourceManager.getDefault().getSelectedResource();
			OrcConfigSettings orcConfig;
			orcConfig = new OrcConfigSettings(orcProgToLaunch.getProject(), configuration);
			orcConfig.filename_$eq(orcProgToLaunch.getRawLocation().toFile().toString());

			final String mainTypeName = "orc.Main"; //$NON-NLS-1$
			final IVMRunner runner = getVMRunner(configuration, mode);

			final File launchConfigWorkingDir = verifyWorkingDirectory(configuration);
			final String workingDirName;
			if (launchConfigWorkingDir != null) {
				workingDirName = launchConfigWorkingDir.getAbsolutePath();
			} else {
				// Default to dir of launched file
				workingDirName = orcProgToLaunch.getParent().getLocation().toOSString();
			}

			// Environment variables
			final String[] envp = getEnvironment(configuration);

			// Program & VM arguments
			final String[] pgmArgArray = orcConfig.composeCmdLine();
			String pgmArgs = ""; //$NON-NLS-1$
			for (final String arg : pgmArgArray) {
				pgmArgs += "\"" + arg.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"") + "\" "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			}
			pgmArgs = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(pgmArgs);
			final String vmArgs = getVMArguments(configuration);
			final ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);

			// VM-specific attributes
			final Map vmAttributesMap = getVMSpecificAttributesMap(configuration);

			// Classpath
			String[] classpath;
			if (!configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true) && getClasspath(configuration).length > 0) {
				classpath = getClasspath(configuration);
			} else {
				classpath = getAbsoluteClasspathForClass(Main.class);
			}

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
			if (monitorNN.isCanceled()) {
				return;
			}

			// done the verification phase
			monitorNN.worked(1);

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitorNN);
			for (final IProcess proc : launch.getProcesses()) {
				final String processLabel = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution("${resource_path}") + " [" + configuration.getType().getName() + "] (" + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date(System.currentTimeMillis())) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				proc.setAttribute(IProcess.ATTR_PROCESS_LABEL, processLabel);
			}

			// check for cancellation
			if (monitorNN.isCanceled()) {
				return;
			}
		} finally {
			monitorNN.done();
		}
	}

	@SuppressWarnings("rawtypes")
	private String[] getAbsoluteClasspathForClass(final Class classOfInterest) {
		//FIXME: Is this possible without using the internal OSGi BaseData and DefaultClassLoader classes?
		final BaseData basedata = ((DefaultClassLoader) classOfInterest.getClassLoader()).getClasspathManager().getBaseData();
		String[] classpath = null;
		try {
			classpath = basedata.getClassPath();
			for (int i = 0; i < classpath.length; i++) {
				final File classpathEntryFile = basedata.getBundleFile().getFile(classpath[i], false);
				if (classpathEntryFile != null) {
					classpath[i] = classpathEntryFile.getAbsolutePath();
				} else {
					// Cannot get the file for this classpath entry.
					// This happens, for example, for "." in a deployed plugin.
					// We'll just guess the bundle location itself, then.
					classpath[i] = basedata.getBundleFile().getBaseFile().getAbsolutePath();
				}
			}
		} catch (final BundleException e) {
			// This is thrown on an invalid JAR manifest, but then we wouldn't be here, so this is an "impossible" case
			Activator.log(e);
		}
		return classpath;
	}

	/*
	 * Derived from org.eclipse.jdt.internal.launching.StandardVMRunner.java,
	 * Revision 1.56 (31 Mar 2009), trunk rev as of 20 Aug 2009 
	 */
	@SuppressWarnings("rawtypes")
	protected static String convertClassPath(final List cp) {
		final StringBuffer buf = new StringBuffer();
		if (cp.size() == 0) {
			return ""; //$NON-NLS-1$
		}
		for (int i = 0; i < cp.size(); i++) {
			if (i > 0) {
				buf.append(File.pathSeparator);
			}
			buf.append(cp.get(i));
		}
		return buf.toString();
	}
}
