//
// OrcBuilder.java -- Java class OrcBuilder
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.build;

import java.io.File;
import java.io.IOException;

import orc.OrcCompiler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.builder.BuilderBase;
import org.eclipse.imp.builder.MarkerCreatorWithBatching;
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
		//TODO: This template code from IMP is bogus
		return resource.getFullPath().lastSegment().equals("bin"); //$NON-NLS-1$
	}

	/**
	 * Compile one Orc file.
	 *
	 * @see org.eclipse.imp.builder.BuilderBase#compile(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void compile(final IFile file, final IProgressMonitor monitor) {

		getConsoleStream().println(Messages.OrcBuilder_BuildingOrcFile + file.getName());

		try {
			final OrcConfigSettings config = new OrcConfigSettings(getProject(), null);

			final File inputFile = new File(file.getLocation().toOSString());
			config.setInputFile(inputFile);
			config.setProgressListener(new ImpToOrcProgressAdapter(monitor));
			config.setMessageRecorder(new ImpToOrcMessageAdapter(new MarkerCreatorWithBatching(file, null, this)));
			file.deleteMarkers(OrcBuilder.PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);

			final OrcCompiler compiler = new OrcCompiler(config);
			try {
				compiler.call();
				// Disregard returned OIL, we just want the errors
			} catch (final IOException e) {
				//TODO: Handle this differently?
				getConsoleStream().println(Messages.OrcBuilder_IOErrorWhenBuilding + file.getName() + ": " + e.getMessage()); //$NON-NLS-1$
				getPlugin().logException(e.getMessage(), e);
			}

			doRefresh(file.getParent()); // N.B.: Assumes all generated files go into parent folder
		} catch (final Exception e) {
			// catch Exception, because any exception could break the
			// builder infrastructure.
			getConsoleStream().println(Messages.OrcBuilder_CompilerInternalErrorOn + file.getName() + ": " + e.getMessage()); //$NON-NLS-1$
			getPlugin().logException(e.getMessage(), e);
		}
		getConsoleStream().println(Messages.OrcBuilder_DoneBuildingOrcFile + file.getName());
		monitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getConsoleName()
	 */
	@Override
	protected String getConsoleName() {
		return Messages.OrcBuilder_OrcCompilerConsoleName;
	}
}
