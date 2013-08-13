//
// OrcBuilder.java -- Java class OrcBuilder
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import orc.compile.StandardOrcCompiler;
import orc.compile.parse.OrcFileInputContext;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.imp.builder.BuilderBase;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.runtime.PluginBase;

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
public class OrcBuilder extends BuilderBase {
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
	 * Name of the language for this builder.
	 */
	public static final String LANGUAGE_NAME = "Orc"; //$NON-NLS-1$

	/**
	 * Language for this builder.
	 */
	public static final Language LANGUAGE = LanguageRegistry.findLanguage(LANGUAGE_NAME);

	/**
	 * Required no-arg constructor for OrcBuilder.
	 *
	 */
	public OrcBuilder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getPlugin()
	 */
	@Override
	protected PluginBase getPlugin() {
		return Activator.getInstance();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getBuilderID()
	 */
	@Override
	public String getBuilderID() {
		return BUILDER_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getErrorMarkerID()
	 */
	@Override
	protected String getErrorMarkerID() {
		//NOTE: BuilderBase requires these three methods to be implemented,
		//      but in reality they must return the same value.
		return PROBLEM_MARKER_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getWarningMarkerID()
	 */
	@Override
	protected String getWarningMarkerID() {
		//NOTE: BuilderBase requires these three methods to be implemented,
		//      but in reality they must return the same value.
		return PROBLEM_MARKER_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getInfoMarkerID()
	 */
	@Override
	protected String getInfoMarkerID() {
		//NOTE: BuilderBase requires these three methods to be implemented,
		//      but in reality they must return the same value.
		return PROBLEM_MARKER_ID;
	}

	/**
	 * Decide whether a file needs to be build using this builder. Note that
	 * <code>isNonRootSourceFile()</code> and <code>isSourceFile()</code> should
	 * never return true for the same file.
	 *
	 * @return true iff an arbitrary file is a Orc source file.
	 * @see org.eclipse.imp.builder.BuilderBase#isSourceFile(org.eclipse.core.resources.IFile)
	 */
	@Override
	protected boolean isSourceFile(final IFile file) {
		final boolean emitDiags = getDiagPreference();

		final IPath path = file.getRawLocation();
		if (path == null) {
			if (emitDiags) {
				getConsoleStream().println("isSourceFile on a null path"); //$NON-NLS-1$
			}
			return false;
		}

		final String pathString = path.toString();
		if (pathString.indexOf("/bin/") != -1) { //$NON-NLS-1$
			if (emitDiags) {
				getConsoleStream().println("isSourceFile(" + file + ")=false, since it contains '/bin/'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return false;
		}

		if (emitDiags) {
			getConsoleStream().println("isSourceFile(" + file + ")=" + (LANGUAGE.hasExtension(path.getFileExtension()) && !isNonRootSourceFile(file))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return LANGUAGE.hasExtension(path.getFileExtension()) && !isNonRootSourceFile(file);
	}

	/**
	 * Decide whether or not to scan a file for dependencies. Note:
	 * <code>isNonRootSourceFile()</code> and <code>isSourceFile()</code> should
	 * never return true for the same file.
	 *
	 * @return true iff the given file is a source file that this builder should
	 *         scan for dependencies, but not compile as a top-level compilation
	 *         unit.
	 * @see org.eclipse.imp.builder.BuilderBase#isNonRootSourceFile(org.eclipse.core.resources.IFile)
	 */
	@Override
	protected boolean isNonRootSourceFile(final IFile file) {
		return Activator.isOrcIncludeFile(file.getFullPath());
	}

	/**
	 * Collects compilation-unit dependencies for the given file, and records
	 * them via calls to <code>fDependency.addDependency()</code>.
	 *
	 * @see org.eclipse.imp.builder.BuilderBase#collectDependencies(org.eclipse.core.resources.IFile)
	 */
	@Override
	protected void collectDependencies(final IFile file) {
		file.getFullPath().toString();

		final boolean emitDiags = getDiagPreference();
		if (emitDiags) {
			getConsoleStream().println("Collecting dependencies from Orc file: " + file.getName()); //$NON-NLS-1$
		}

		// TODO: implement dependency collector
		// E.g. for each dependency:
		// fDependencyInfo.addDependency(fromPath, uponPath);
	}

	/**
	 * @return true iff this resource identifies the output folder
	 * @see org.eclipse.imp.builder.BuilderBase#isOutputFolder(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected boolean isOutputFolder(final IResource resource) {
		return false; // No output folders in Orc projects (for now)
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected IProject[] build(final int kind, final Map args, final IProgressMonitor monitor_) {
		// This override is only needed as an IMP bug fix
		// When IMP's BuilderBase.build correctly manages the IProgressMonitor, remove this override.
		final IProgressMonitor monitor = monitor_ == null ? new NullProgressMonitor() : monitor_;
		checkCancel(monitor);
		monitor.beginTask(Messages.OrcBuilder_Preparing + getProject().getName(), 100000);
		final IProject[] requiredProjects = super.build(kind, args, monitor);
		monitor.subTask(Messages.OrcBuilder_Done);
		monitor.done();
		return requiredProjects;
	}

	/**
	 * Compile one Orc file.
	 *
	 * @see org.eclipse.imp.builder.BuilderBase#compile(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void compile(final IFile file, final IProgressMonitor monitor_) {
		checkCancel(monitor_);

		// This block is only needed as an IMP bug fix
		// When IMP's BuilderBase.compileNecessarySources correctly passes us a fresh SubProgressMonitor, remove this block.
		final IProgressMonitor monitor;
		try {
			// Violate access controls of parent's fSourcesToCompile field
			final Field fSourcesToCompileField = BuilderBase.class.getDeclaredField("fSourcesToCompile"); //$NON-NLS-1$
			fSourcesToCompileField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final int numFiles = ((Collection<IFile>) fSourcesToCompileField.get(this)).size();

			monitor = new SubProgressMonitor(monitor_, 100000 / numFiles);
		} catch (final IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (final NoSuchFieldException e) {
			throw new AssertionError(e);
		}
		// End bug fix block

		final String fileName = file.getFullPath().makeRelative().toString();

		final boolean emitDiags = getDiagPreference();
		if (emitDiags) {
			getConsoleStream().println(Messages.OrcBuilder_BuildingOrcFile + fileName);
		}
		monitor.beginTask(Messages.OrcBuilder_Compiling + fileName, 1000);
		monitor.subTask(Messages.OrcBuilder_Compiling + fileName);
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

			doRefresh(file.getParent()); // N.B.: Assumes all generated files go into parent folder
		} catch (final Exception e) {
			// catch Exception, because any exception could break the
			// builder infrastructure.
			getConsoleStream().println(Messages.OrcBuilder_CompilerInternalErrorOn + fileName + ": " + e.toString()); //$NON-NLS-1$
			Activator.log(e);
		} finally {
			monitor.done();
		}
		if (emitDiags) {
			getConsoleStream().println(Messages.OrcBuilder_DoneBuildingOrcFile + fileName);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getConsoleName()
	 */
	@Override
	protected String getConsoleName() {
		return Messages.OrcBuilder_OrcCompilerConsoleName;
	}

	/**
	 * Check whether the build has been canceled.
	 *
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	public void checkCancel(final IProgressMonitor monitor) throws OperationCanceledException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
		IProject currentProject = getProject();
		if (currentProject == null || !currentProject.isAccessible() || !currentProject.exists()) return;

		currentProject.deleteMarkers(OrcBuilder.PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);

		if (monitor != null)
			monitor.done();
	}
}
