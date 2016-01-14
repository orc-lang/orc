//
// OrcLaunchDelegate.java -- Java class OrcLaunchDelegate
// Project OrcEclipse
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
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

import org.eclipse.core.resources.IProject;
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
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.ui.statushandlers.StatusManager;

import orc.Main;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

/**
 * Launches an Orc program.
 * <p>
 * A launch configuration delegate performs launching for a specific type of
 * launch configuration. A launch configuration delegate is defined by the
 * <code>delegate</code> attribute of a <code>launchConfigurationType</code>
 * extension.
 *
 * @author jthywiss
 */
@SuppressWarnings("restriction")
public class OrcLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
    private IProject[] referencedProjectsInBuildOrder;
    private IResource currentLaunchOrcProg;

    /**
     * Launch configuration extension type ID for an Orc Program Launch
     * configuration
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
     * @throws CoreException
     */
    public static void setDefaults(final ILaunchConfigurationWorkingCopy configuration) throws CoreException {
        // Currently, a minimal Orc launch config. is an nearly empty one.
        // We turn off background launching by default because launching can be slow and non-obvious.
        configuration.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
        configuration.doSave();
    }

    /**
     * @return The Orc program we'll attempt to launch
     */
    protected IResource orcProgToLaunch() {
        return SelectedResourceManager.getDefault().getSelectedResource();
    }

    @Override
    public boolean preLaunchCheck(final ILaunchConfiguration configuration, final String mode, final IProgressMonitor monitor) throws CoreException {
        referencedProjectsInBuildOrder = null;
        currentLaunchOrcProg = orcProgToLaunch();
        if (currentLaunchOrcProg != null) {
            referencedProjectsInBuildOrder = computeReferencedBuildOrder(new IProject[] { currentLaunchOrcProg.getProject() });
        } else {
            StatusManager.getManager().handle(new Status(IStatus.INFO, Activator.getInstance().getID(), 1, Messages.OrcLaunchDelegate_UnableToLaunchNoResourceSelected, null), StatusManager.SHOW);
            return false;
        }
        // do generic launch checks
        return super.preLaunchCheck(configuration, mode, monitor);
    }

    @Override
    protected IProject[] getBuildOrder(final ILaunchConfiguration configuration, final String mode) throws CoreException {
        return referencedProjectsInBuildOrder;
    }

    @Override
    protected IProject[] getProjectsForProblemSearch(final ILaunchConfiguration configuration, final String mode) throws CoreException {
        return referencedProjectsInBuildOrder;
    }

    @Override
    public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, final IProgressMonitor monitor) throws CoreException {
        if (currentLaunchOrcProg == null) {
            StatusManager.getManager().handle(new Status(IStatus.INFO, Activator.getInstance().getID(), 1, Messages.OrcLaunchDelegate_UnableToLaunchNoResourceSelected, null), StatusManager.SHOW);
            return;
        }

        // Derived from org.eclipse.jdt.launching.JavaLaunchDelegate.java,
        // Revision 1.8 (02 Oct 2007), trunk rev as of 04 Aug 2009

        IProgressMonitor monitorNN = monitor;
        if (monitorNN == null) {
            monitorNN = new NullProgressMonitor();
        }
        monitorNN.beginTask(MessageFormat.format("{0}...", new Object[] { configuration.getName() }), 2); //$NON-NLS-1$
        // check for cancellation
        if (monitorNN.isCanceled()) {
            return;
        }
        try {
            monitorNN.subTask(Messages.OrcLaunchDelegate_VerifyingLaunchAttributes);

            OrcConfigSettings orcConfig;
            orcConfig = new OrcConfigSettings(currentLaunchOrcProg.getProject(), configuration);
            orcConfig.filename_$eq(currentLaunchOrcProg.getRawLocation().toFile().toString());

            final Class<?> mainTypeClass = Main.class;
            final IVMRunner runner = getVMRunner(configuration, mode);

            final File launchConfigWorkingDir = verifyWorkingDirectory(configuration);
            final String workingDirName;
            if (launchConfigWorkingDir != null) {
                workingDirName = launchConfigWorkingDir.getAbsolutePath();
            } else {
                // Default to dir of launched file
                workingDirName = currentLaunchOrcProg.getParent().getLocation().toOSString();
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
            final Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

            // Classpath
            String[] classpath;
            if (!configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true) && getClasspath(configuration).length > 0) {
                classpath = getClasspath(configuration);
            } else {
                classpath = getAbsoluteClasspathForClass(mainTypeClass);
            }

            // Create VM config
            final VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeClass.getName(), classpath);
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

    private String[] getAbsoluteClasspathForClass(final Class<?> classOfInterest) {
        //FIXME: Is this possible without using the internal Eclipse OSGi ModuleClassLoader, ClasspathManager, and ClasspathEntry classes?
        final ClasspathManager manager = ((ModuleClassLoader) classOfInterest.getClassLoader()).getClasspathManager();
        String[] classpath = null;
        final ClasspathEntry[] classpathentries = manager.getHostClasspathEntries();
        classpath = new String[classpathentries.length];
        for (int i = 0; i < classpathentries.length; i++) {
            classpath[i] = classpathentries[i].getBundleFile().getBaseFile().getAbsolutePath();
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
