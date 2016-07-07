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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import orc.compile.StandardOrcCompiler;
import orc.compile.parse.OrcFileInputContext;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.ImpToOrcMessageAdapter;
import edu.utexas.cs.orc.orceclipse.ImpToOrcProgressAdapter;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

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
    public static final String BUILDER_ID = Activator.getInstance().getID() + ".build.orcBuilder"; //$NON-NLS-1$

    /**
     * A marker ID that identifies problems detected by the builder
     */
    public static final String PROBLEM_MARKER_ID = Activator.getInstance().getID() + ".problemmarker"; //$NON-NLS-1$

    /**
     * A marker ID that identifies parse problems detected by the builder
     */
    public static final String PARSE_PROBLEM_MARKER_ID = Activator.getInstance().getID() + ".parse.problemmarker"; //$NON-NLS-1$

    /**
     * A marker attribute name for the compile message code attribute
     */
    public static final String COMPILE_EXCEPTION_NAME = Activator.getInstance().getID() + ".compileexceptionname"; //$NON-NLS-1$

    /**
     * The MessageConsoleStream used for Orc builder and compiler output.
     */
    private MessageConsoleStream orcBuildConsoleStream;

    protected static int BUILD_TOTAL_WORK = 100000;

    private static boolean DEBUG = false;

    public OrcBuilder() {
        super();
    }

    /**
     * Runs this builder in the specified manner.
     * <p>
     * If the build kind is {@link #INCREMENTAL_BUILD} or {@link #AUTO_BUILD},
     * the <code>getDelta</code> method can be used during the invocation of
     * this method to obtain information about what changes have occurred since
     * the last invocation of this method. Any resource delta acquired is valid
     * only for the duration of the invocation of this method. A
     * {@link #FULL_BUILD} has no associated build delta.
     * </p>
     * <p>
     * After completing a build, this builder may return a list of projects for
     * which it requires a resource delta the next time it is run. This
     * builder's project is implicitly included and need not be specified. The
     * build mechanism will attempt to maintain and compute deltas relative to
     * the identified projects when asked the next time this builder is run.
     * Builders must re-specify the list of interesting projects every time they
     * are run as this is not carried forward beyond the next build. Projects
     * mentioned in return value but which do not exist will be ignored and no
     * delta will be made available for them.
     * </p>
     * <p>
     * This method is long-running; progress and cancellation are provided by
     * the given progress monitor. All builders should report their progress and
     * honor cancel requests in a timely manner. Cancelation requests should be
     * propagated to the caller by throwing
     * <code>OperationCanceledException</code>.
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
     *            <ul>
     *            <li>{@link #FULL_BUILD} - indicates a full build.</li>
     *            <li>{@link #INCREMENTAL_BUILD}- indicates an incremental
     *            build.</li>
     *            <li>{@link #AUTO_BUILD} - indicates an automatically triggered
     *            incremental build (autobuilding on).</li>
     *            </ul>
     * @param args a table of builder-specific arguments keyed by argument name
     *            (key type: <code>String</code>, value type:
     *            <code>String</code>); <code>null</code> is equivalent to an
     *            empty map
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. This method will call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor, so it must be newly created. Accepts
     *            <code>null</code>, indicating that no progress should be
     *            reported and that the operation cannot be cancelled.
     * @return the list of projects for which this builder would like deltas the
     *         next time it is run or <code>null</code> if none
     * @exception CoreException if this build fails.
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
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @throws CoreException
     */
    protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
        checkCancel(monitor);
        monitor.subTask(Messages.OrcBuilder_Preparing);
        final Set<IFile> collectedResources = startCollectingResources(monitor);
        getProject().accept(new IResourceVisitor() {
            @Override
            public boolean visit(final IResource resource) throws CoreException {
                return collectResource(resource, collectedResources, monitor);
            }
        });
        finishCollectingResources(collectedResources, monitor);
        monitor.worked(BUILD_TOTAL_WORK / 20);
        monitor.subTask(""); //$NON-NLS-1$
        buildFromResources(collectedResources, monitor);
    }

    /**
     * @param delta
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @throws CoreException
     */
    protected void incrementalBuild(final IResourceDelta delta, final IProgressMonitor monitor) throws CoreException {
        checkCancel(monitor);
        monitor.subTask(Messages.OrcBuilder_Preparing);
        // FIXME: Need to build dependencies DAG and propagate changes
        final Set<IFile> collectedResources = startCollectingResources(monitor);
        delta.accept(new IResourceDeltaVisitor() {
            @Override
            public boolean visit(final IResourceDelta delta) throws CoreException {
                final IResource resource = delta.getResource();
                boolean visitChidren = true;
                switch (delta.getKind()) {
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
            }
        });
        finishCollectingResources(collectedResources, monitor);
        monitor.worked(5000);
        monitor.subTask(""); //$NON-NLS-1$
        buildFromResources(collectedResources, monitor);
    }

    /**
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
     * that has been computed as a result of previous builds. It is recommended
     * that builders override this method to delete all derived resources
     * created by previous builds, and to remove all markers of type
     * {@link IMarker#PROBLEM} that were created by previous invocations of the
     * builder. The platform will take care of discarding the builder's last
     * built state (there is no need to call <code>forgetLastBuiltState</code>).
     * </p>
     * <p>
     * This method is called as a result of invocations of
     * <code>IWorkspace.build</code> or <code>IProject.build</code> where the
     * build kind is {@link #CLEAN_BUILD}.
     * <p>
     * This method is long-running; progress and cancellation are provided by
     * the given progress monitor. All builders should report their progress and
     * honor cancel requests in a timely manner. Cancellation requests should be
     * propagated to the caller by throwing
     * <code>OperationCanceledException</code>.
     * </p>
     *
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. This method will call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor, so it must be newly created. Accepts
     *            <code>null</code>, indicating that no progress should be
     *            reported and that the operation cannot be cancelled.
     * @exception CoreException if this build fails.
     * @see IWorkspace#build(int, IProgressMonitor)
     * @see #CLEAN_BUILD
     * @since 3.0
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
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @return Fresh resource container to be used for builder
     */
    protected Set<IFile> startCollectingResources(final IProgressMonitor monitor) {
        return new HashSet<IFile>();
    }

    /**
     * @param resource
     * @param collectedResources
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @return <code>true</code> if the resource's members should be visited;
     *         <code>false</code> if they should be skipped.
     * @throws CoreException
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
     * @param collectedResources
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     */
    protected void finishCollectingResources(final Set<IFile> collectedResources, final IProgressMonitor monitor) {
        // Nothing needed at the moment
    }

    /**
     * @param collectedResources
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. It is the caller's responsibility to call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor. Must not be <code>null</code>.
     * @throws CoreException
     */
    protected void buildFromResources(final Set<IFile> collectedResources, final IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        final int workPerResource = (BUILD_TOTAL_WORK - BUILD_TOTAL_WORK / 20) / collectedResources.size();
        final SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, workPerResource);
        for (final IFile file : collectedResources) {
            file.deleteMarkers(OrcBuilder.PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
            compile(file, subMonitor);
        }
    }

    /**
     * Decide whether a file needs to be build using this builder. Note that
     * <code>isNonRootSourceFile()</code> and <code>isSourceFile()</code> should
     * never return true for the same file.
     *
     * @param file
     * @return true iff an arbitrary file is a Orc source file.
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

        final boolean result = Activator.isOrcSourceFile(path) && !isNonRootSourceFile(file);
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
     * @param file
     * @return true iff the given file is a source file that this builder should
     *         scan for dependencies, but not compile as a top-level compilation
     *         unit.
     */
    protected boolean isNonRootSourceFile(final IFile file) {
        return Activator.isOrcIncludeFile(file.getFullPath());
    }

    /**
     * @param resource
     * @return true iff this resource identifies the output folder
     */
    protected boolean isOutputFolder(final IResource resource) {
        return false; // No output folders in Orc projects (for now)
    }

    /**
     * Compile one Orc file.
     *
     * @param file
     * @param monitor the progress monitor to use for reporting progress to the
     *            user. This method will call
     *            <code>beginTask(String, int)</code> and <code>done()</code> on
     *            the given monitor, so it must be newly created. Must not be
     *            <code>null</code>.
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
            final ImpToOrcProgressAdapter prgsLstnr = new ImpToOrcProgressAdapter(new SubProgressMonitor(monitor, 1000));
            final ImpToOrcMessageAdapter compileLogger = new ImpToOrcMessageAdapter(BUILDER_ID, false);

            try {
                new StandardOrcCompiler().apply(ic, config, compileLogger, prgsLstnr);
                // Disregard returned OIL, we just want the errors
            } catch (final IOException e) {
                getConsoleStream().println(Messages.OrcBuilder_IOErrorWhenBuilding + fileName + ": " + e.toString()); //$NON-NLS-1$
                Activator.logAndShow(e);
            }

            // TODO: If/when we generate compile output, refresh the appropriate
            // Resources

        } catch (final Exception e) {
            // catch Exception, because any exception could break the
            // builder infrastructure.
            getConsoleStream().println(Messages.OrcBuilder_CompilerInternalErrorOn + fileName + ": " + e.toString()); //$NON-NLS-1$
            Activator.log(e);
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
     * @throws OperationCanceledException
     */
    public void checkCancel(final IProgressMonitor monitor) throws OperationCanceledException {
        if (monitor != null && monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /**
     * @return
     */
    protected MessageConsoleStream getConsoleStream() {
        if (orcBuildConsoleStream == null) {
            orcBuildConsoleStream = new MessageConsole(Messages.OrcBuilder_OrcCompilerConsoleName, null /* imageDescriptor */).newMessageStream();
        }
        return orcBuildConsoleStream;
    }
}
