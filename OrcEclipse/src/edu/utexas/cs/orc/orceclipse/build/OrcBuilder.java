//
// OrcBuilder.java -- Java class OrcBuilder
// Project OrcEclipse
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import orc.compile.StandardOrcCompiler;
import orc.compile.parse.OrcFileInputContext;

import edu.utexas.cs.orc.orceclipse.EclipseToOrcMessageAdapter;
import edu.utexas.cs.orc.orceclipse.EclipseToOrcProgressAdapter;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;
import edu.utexas.cs.orc.orceclipse.OrcPlugin;
import edu.utexas.cs.orc.orceclipse.OrcResources;

/**
 * Incremental builder for Orc source code.
 * <p>
 * A builder may be activated on a file containing Orc code every time it has
 * changed (when "Build automatically" is on), or when the programmer chooses to
 * "Build" a project.
 *
 * @see org.eclipse.core.resources.IncrementalProjectBuilder
 */
public class OrcBuilder extends IncrementalProjectBuilder {
    /**
     * Extension ID of the Orc builder, which matches the ID in the
     * corresponding extension definition in plugin.xml.
     */
    public static final String BUILDER_ID = OrcPlugin.getID() + ".build.orcBuilder"; //$NON-NLS-1$

    /**
     * A marker ID that identifies problems detected by the builder
     */
    public static final String PROBLEM_MARKER_ID = OrcPlugin.getID() + ".problemmarker"; //$NON-NLS-1$

    /**
     * A marker ID that identifies parse problems detected by the builder
     */
    public static final String PARSE_PROBLEM_MARKER_ID = OrcPlugin.getID() + ".parse.problemmarker"; //$NON-NLS-1$

    /**
     * A marker attribute name for the compile message code attribute
     */
    public static final String COMPILE_EXCEPTION_NAME = OrcPlugin.getID() + ".compileexceptionname"; //$NON-NLS-1$

    private static ImageDescriptor ORC_PLUGIN_ICON_IMAGE_DESCRIPTOR = OrcPlugin.getInstance().getImageRegistry().getDescriptor(OrcResources.ORC_PLUGIN_ICON);

    /**
     * The MessageConsoleStream used for Orc builder and compiler output.
     */
    private MessageConsoleStream orcBuildConsoleStream;

    protected static int BUILD_TOTAL_WORK = 100000;

    private static boolean DEBUG = false;

    /**
     * Constructs an OrcBuilder. Builder instances are managed by Eclipse, which
     * requires a public, no-argument constructor.
     */
    public OrcBuilder() {
        super();
    }

    /**
     * Runs this builder in the specified manner.
     * <p>
     * If the build kind is <code>INCREMENTAL_BUILD</code> or
     * <code>AUTO_BUILD</code>, the <code>getDelta</code> method can be used
     * during the invocation of this method to obtain information about what
     * changes have occurred since the last invocation of this method. Any
     * resource delta acquired is valid only for the duration of the invocation
     * of this method. A <code>FULL_BUILD</code> has no associated build delta.
     * </p>
     * <p>
     * After completing a build, this builder may return a list of projects for
     * which it requires a resource delta the next time it is run.
     * </p>
     * <p>
     * This method is long-running; progress and cancellation are provided by
     * the given progress monitor. Cancellation requests are propagated to the
     * caller by throwing <code>OperationCanceledException</code>.
     * </p>
     * <p>
     * All builders should try to be robust in the face of trouble. In
     * situations where failing the build by throwing <code>CoreException</code>
     * is the only option, a builder has a choice of how best to communicate the
     * problem back to the caller. One option is to use the
     * {@link IResourceStatus#BUILD_FAILED} status code along with a suitable
     * message; another is to use a {@link MultiStatus} containing finer-grained
     * problem diagnoses.
     * </p>
     *
     * @param kind the kind of build being requested. Valid values are
     *            FULL_BUILD, INCREMENTAL_BUILD, and AUTO_BUILD.
     * @param args a table of builder-specific arguments. Ignored by this
     *            builder.
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. This method will call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor, so it must be newly created. Accepts
     *            <code>null</code>, indicating that no progress should be
     *            reported and that the operation cannot be cancelled.
     * @return the list of projects for which this builder would like deltas the
     *         next time it is run or <code>null</code> if none
     * @exception CoreException if this build fails
     * @exception OperationCanceledException when operation is cancelled
     * @see IProject#build(int, String, Map, IProgressMonitor)
     */
    @Override
    protected IProject[] build(final int kind, final Map<String, String> args, final IProgressMonitor monitor_) throws CoreException {
        /*
         * monitor state from the build manager: freshly allocated, with
         * subTask("Invoking builder on projName") called
         */
        final IProgressMonitor monitor = monitor_ == null ? new NullProgressMonitor() : monitor_;
        try {
            checkCancel(monitor);
            // TODO: isInterrupted? (For autobuilds only.)
            monitor.beginTask(Messages.OrcBuilder_BuildingProject + getProject().getName(), BUILD_TOTAL_WORK);
            if (kind == FULL_BUILD) {
                fullBuild(monitor);
            } else /* INCREMENTAL_BUILD or AUTO_BUILD */{
                final IResourceDelta delta = getDelta(getProject());
                if (delta == null) {
                    fullBuild(monitor);
                } else {
                    incrementalBuild(delta, monitor);
                }
            }
            final IProject[] requiredProjects = getRequiredProjects(monitor);
            return requiredProjects;
        } finally {
            monitor.subTask(Messages.OrcBuilder_Done);
            monitor.done();
        }
    }

    /**
     * Perform a full build. A full build discards all previously built state
     * and builds all resources again. Resource deltas are not applicable for
     * this kind of build.
     *
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @exception CoreException if this build fails
     * @exception OperationCanceledException when operation is cancelled
     */
    protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
        checkCancel(monitor);
        monitor.subTask(Messages.OrcBuilder_Preparing);
        final Set<IFile> collectedResources = startCollectingResources(monitor);
        getProject().accept(resource -> collectResource(resource, collectedResources, monitor));
        final List<IFile> filesToBuild = computeFilesToBuild(collectedResources, monitor);
        monitor.worked(BUILD_TOTAL_WORK / 20);
        monitor.subTask(""); //$NON-NLS-1$
        buildFiles(filesToBuild, monitor);
    }

    /**
     * Perform an incremental build. Incremental builds use an
     * {@link IResourceDelta} that describes what resources have changed since
     * the last build. The builder calculates what resources are affected by the
     * delta, and rebuilds the affected resources.
     *
     * @param delta
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @exception CoreException if this build fails
     * @exception OperationCanceledException when operation is cancelled
     */
    protected void incrementalBuild(final IResourceDelta delta, final IProgressMonitor monitor) throws CoreException {
        checkCancel(monitor);
        monitor.subTask(Messages.OrcBuilder_Preparing);
        final Set<IFile> collectedResources = startCollectingResources(monitor);
        delta.accept(delta1 -> {
            final IResource resource = delta1.getResource();
            boolean visitChidren = true;
            switch (delta1.getKind()) {
            case IResourceDelta.ADDED:
                // handle added resource
                visitChidren = collectResource(resource, collectedResources, monitor);
                break;
            case IResourceDelta.REMOVED:
                // handle removed resource
                break;
            case IResourceDelta.CHANGED:
                // handle changed resource
                visitChidren = collectResource(resource, collectedResources, monitor);
                break;
            }
            return visitChidren;
        });
        final List<IFile> filesToBuild = computeFilesToBuild(collectedResources, monitor);
        monitor.worked(BUILD_TOTAL_WORK / 20);
        monitor.subTask(""); //$NON-NLS-1$
        buildFiles(filesToBuild, monitor);
    }

    /**
     * Get a list of projects for which this builder requires a resource delta
     * the next time it is run. This builder's project is implicitly included
     * and need not be specified. The build mechanism will attempt to maintain
     * and compute deltas relative to the identified projects when asked the
     * next time this builder is run. Builders must re-specify the list of
     * interesting projects every time they are run as this is not carried
     * forward beyond the next build. Projects mentioned in return value but
     * which do not exist will be ignored and no delta will be made available
     * for them.
     *
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @return the list of projects for which this builder would like deltas the
     *         next time it is run or <code>null</code> if none
     */
    protected IProject[] getRequiredProjects(final IProgressMonitor monitor) {
        return null;
    }

    /**
     * Clean is an opportunity for a builder to discard any additional state
     * that has been computed as a result of previous builds. This method
     * deletes all derived resources created by previous builds, and removes all
     * markers of type {@link IMarker#PROBLEM} that were created by previous
     * invocations of the builder. The platform will take care of discarding the
     * builder's last built state (there is no need to call
     * <code>forgetLastBuiltState</code>). </p>
     * <p>
     * This method is called as a result of invocations of
     * <code>IWorkspace.build</code> or <code>IProject.build</code> where the
     * build kind is <code>CLEAN_BUILD</code>.
     * <p>
     * This method is long-running; progress and cancellation are provided by
     * the given progress monitor. Cancellation requests are propagated to the
     * caller by throwing <code>OperationCanceledException</code>.
     * </p>
     *
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. This method will call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor, so it must be newly created. Accepts
     *            <code>null</code>, indicating that no progress should be
     *            reported and that the operation cannot be cancelled.
     * @exception CoreException if this build fails
     * @exception OperationCanceledException when operation is cancelled
     */
    @Override
    protected void clean(final IProgressMonitor monitor) throws CoreException {
        super.clean(monitor);
        try {
            checkCancel(monitor);
            if (monitor != null) {
                monitor.beginTask(NLS.bind(Messages.OrcBuilder_CleaningProject, getProject().getName()), BUILD_TOTAL_WORK);
            }
            final IProject currentProject = getProject();
            if (currentProject == null || !currentProject.isAccessible() || !currentProject.exists()) {
                return;
            }

            currentProject.deleteMarkers(OrcBuilder.PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);

        } finally {
            if (monitor != null) {
                monitor.worked(BUILD_TOTAL_WORK);
                monitor.subTask(Messages.OrcBuilder_Done);
                monitor.done();
            }
        }
    }

    /**
     * Allocate a container for collectResource calls.
     *
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @return fresh resource container to be used for collectResource calls
     * @see #collectResource(IResource, Set, IProgressMonitor)
     */
    protected Set<IFile> startCollectingResources(final IProgressMonitor monitor) {
        return new HashSet<IFile>();
    }

    /**
     * Examine a resource that may need to be rebuilt, and add it (if needed) to
     * the given collection of to-be-rebuilt resources.
     *
     * @param resource A changed IResource
     * @param collectedResources the Set of to-be-rebuilt IFiles up to now,
     *            which may be modified if resource needs to be rebuilt
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @return <code>true</code> if the resource's members should be visited;
     *         <code>false</code> if they should be skipped.
     * @exception CoreException if this build fails
     * @exception OperationCanceledException when operation is cancelled
     */
    protected boolean collectResource(final IResource resource, final Set<IFile> collectedResources, final IProgressMonitor monitor) throws CoreException {
        checkCancel(monitor);
        if (resource instanceof IFile) {
            final IFile file = (IFile) resource;

            if (file.exists()) {
                if (isSourceFile(file) || isNonRootSourceFile(file)) {
                    collectedResources.add(file);
                }
            }
            return false;
        } else if (isOutputFolder(resource)) {
            return false;
        }
        return true;
    }

    /**
     * Transform the set of changed files into a list of files to compile.
     *
     * @param collectedResources the Set of putative changed IFiles
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @returns a Set of to-be-rebuilt IFiles
     * @see #collectResource(IResource, Set, IProgressMonitor)
     */
    protected List<IFile> computeFilesToBuild(final Set<IFile> collectedResources, final IProgressMonitor monitor) {
        final List<IFile> filesToBuild = new ArrayList<>();
        // FIXME: Need to build dependencies DAG and propagate changes
        for (final IFile file : collectedResources) {
            if (isSourceFile(file)) {
                filesToBuild.add(file);
            }
        }
        return filesToBuild;
    }

    /**
     * From a collection of to-be-rebuilt resources, call compiler to build
     * project.
     *
     * @param collectedResources a Set of to-be-rebuilt IFiles
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @exception CoreException if this build fails
     * @exception OperationCanceledException when operation is cancelled
     */
    protected void buildFiles(final List<IFile> filesToBuild, final IProgressMonitor monitor) throws CoreException {
        final int workPerResource = (BUILD_TOTAL_WORK - BUILD_TOTAL_WORK / 20) / Math.max(filesToBuild.size(), 1);
        final SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, workPerResource);
        for (final IFile file : filesToBuild) {
            file.deleteMarkers(OrcBuilder.PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
            compile(file, subMonitor);
        }
    }

    /**
     * Decide whether a file needs to be build using this builder. Note that
     * <code>isNonRootSourceFile()</code> and <code>isSourceFile()</code> should
     * never return true for the same file.
     *
     * @param file the IFile to check
     * @return true iff an arbitrary file is a Orc source file
     */
    protected boolean isSourceFile(final IFile file) {

        final IPath path = file.getRawLocation();
        if (path == null) {
            if (DEBUG) {
                getConsoleStream().println("isSourceFile on a null path"); //$NON-NLS-1$
            }
            return false;
        }

        final String pathString = path.toString();
        if (pathString.indexOf("/bin/") != -1) { //$NON-NLS-1$
            if (DEBUG) {
                getConsoleStream().println("isSourceFile(" + file + ")=false, since it contains '/bin/'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return false;
        }

        final boolean result = OrcPlugin.isOrcSourceFile(file) && !isNonRootSourceFile(file);
        if (DEBUG) {
            getConsoleStream().println("isSourceFile(" + file + ")=" + result); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return result;
    }

    /**
     * Decide whether or not to scan a file for dependencies. Note:
     * <code>isNonRootSourceFile()</code> and <code>isSourceFile()</code> should
     * never return true for the same file.
     *
     * @param file the IFile to check
     * @return true iff the given file is a source file that this builder should
     *         scan for dependencies, but not compile as a top-level compilation
     *         unit.
     */
    protected boolean isNonRootSourceFile(final IFile file) {
        return OrcPlugin.isOrcIncludeFile(file);
    }

    /**
     * Check if a resource is a build output folder.
     *
     * @param resource the IResource to check
     * @return true iff this resource identifies an output folder
     */
    protected boolean isOutputFolder(final IResource resource) {
        return false; // No output folders in Orc projects (for now)
    }

    /**
     * Compile one Orc file.
     *
     * @param file the IFile to compile
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. This method will call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor, so it must be newly created. Must not be
     *            <code>null</code>.
     * @exception OperationCanceledException when operation is cancelled
     */
    protected void compile(final IFile file, final IProgressMonitor monitor) {
        checkCancel(monitor);

        final String fileName = file.getFullPath().makeRelative().toString();

        if (DEBUG) {
            getConsoleStream().println(Messages.OrcBuilder_BuildingOrcFile + fileName);
        }
        monitor.beginTask(NLS.bind(Messages.OrcBuilder_Compiling, fileName), 1000);
        monitor.subTask(NLS.bind(Messages.OrcBuilder_Compiling, fileName));
        try {
            final OrcConfigSettings config = new OrcConfigSettings(getProject(), null);
            config.filename_$eq(file.getLocation().toOSString());
            final OrcFileInputContext ic = new OrcFileInputContext(new File(file.getLocation().toOSString()), file.getCharset());
            final EclipseToOrcProgressAdapter prgsLstnr = new EclipseToOrcProgressAdapter(new SubProgressMonitor(monitor, 1000));
            final EclipseToOrcMessageAdapter compileLogger = new EclipseToOrcMessageAdapter(BUILDER_ID, false);

            try {
                new StandardOrcCompiler().apply(ic, config, compileLogger, prgsLstnr);
                // Disregard returned OIL, we just want the errors
            } catch (final IOException e) {
                getConsoleStream().println(Messages.OrcBuilder_IOErrorWhenBuilding + fileName + ": " + e.toString()); //$NON-NLS-1$
                OrcPlugin.logAndShow(e);
            }

            // TODO: If/when we generate compile output, refresh the appropriate resources

        } catch (final Exception e) {
            // catch Exception, because any exception could break the
            // builder infrastructure.
            getConsoleStream().println(Messages.OrcBuilder_CompilerInternalErrorOn + fileName + ": " + e.toString()); //$NON-NLS-1$
            OrcPlugin.log(e);
        } finally {
            monitor.done();
        }
        if (DEBUG) {
            getConsoleStream().println(Messages.OrcBuilder_DoneBuildingOrcFile + fileName);
        }
    }

    /**
     * Check whether the build has been canceled.
     *
     * @param monitor the progress monitor to check. Accepts <code>null</code>,
     *            indicating that the operation cannot be cancelled.
     * @throws OperationCanceledException when monitor is requesting
     *             cancellation
     */
    public void checkCancel(final IProgressMonitor monitor) throws OperationCanceledException {
        if (monitor != null && monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /**
     * Get the output stream for Orc builder message output.
     *
     * @return the MessageConsoleStream
     */
    protected MessageConsoleStream getConsoleStream() {
        if (orcBuildConsoleStream == null) {
            final MessageConsole orcBuildConsole = new MessageConsole(Messages.OrcBuilder_OrcCompilerConsoleName, ORC_PLUGIN_ICON_IMAGE_DESCRIPTOR);
            ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { orcBuildConsole });
            orcBuildConsoleStream = orcBuildConsole.newMessageStream();
        }
        return orcBuildConsoleStream;
    }
}
