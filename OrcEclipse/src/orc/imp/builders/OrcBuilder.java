//
// OrcBuilder.java -- Java class OrcBuilder
// Project OrcEclipse
//
// $Id: OrcBuilder.java 1230 2009-08-18 14:58:16Z jthywissen $
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.imp.builders;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import orc.Config;
import orc.Orc;
import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
import orc.ast.oil.xml.Oil;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.CompileMessageRecorder.Severity;
import orc.progress.SubProgressListener;
import orc.runtime.nodes.Pub;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.builder.BuilderBase;
import org.eclipse.imp.builder.MarkerCreator;
import org.eclipse.imp.builder.MarkerCreatorWithBatching;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.runtime.PluginBase;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.ImpToOrcMessageAdapter;
import edu.utexas.cs.orc.orceclipse.ImpToOrcProgressAdapter;

/**
 * Incremental builder for Orc source code.
 * <p>
 * A builder may be activated on a file containing Orc code every time it has
 * changed (when "Build automatically" is on), or when the programmer chooses to
 * "Build" a project. 
 */
public class OrcBuilder extends BuilderBase {
	/**
	 * Extension ID of the Orc builder, which matches the ID in the
	 * corresponding extension definition in plugin.xml.
	 */
	public static final String BUILDER_ID = Activator.getInstance().getID() + ".orc.imp.builder";

	/**
	 * A marker ID that identifies problems detected by the builder
	 */
	public static final String PROBLEM_MARKER_ID = Activator.getInstance().getID() + ".problemmarker";

	/**
	 * Name of the language for this builder.
	 */
	public static final String LANGUAGE_NAME = "Orc";

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
				getConsoleStream().println("isSourceFile on a null path");
			}
			return false;
		}

		final String pathString = path.toString();
		if (pathString.indexOf("/bin/") != -1) {
			if (emitDiags) {
				getConsoleStream().println("isSourceFile(" + file + ")=false, since it contains '/bin/'");
			}
			return false;
		}

		if (emitDiags) {
			getConsoleStream().println("isSourceFile(" + file + ")=" + LANGUAGE.hasExtension(path.getFileExtension()));
		}
		return LANGUAGE.hasExtension(path.getFileExtension());
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
	protected boolean isNonRootSourceFile(final IFile resource) {
		return false;
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
			getConsoleStream().println("Collecting dependencies from Orc file: " + file.getName());
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
		return resource.getFullPath().lastSegment().equals("bin");
	}

	/**
	 * Compile one Orc file.
	 *
	 * @see org.eclipse.imp.builder.BuilderBase#compile(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void compile(final IFile file, final IProgressMonitor monitor) {

		getConsoleStream().println("Building Orc file: " + file.getName());

		try {
			final ImpToOrcProgressAdapter progress = new ImpToOrcProgressAdapter(monitor);

			final MarkerCreator markerCreator = new MarkerCreatorWithBatching(file, null, this);

			final Config config = new Config();
			final File inputFile = new File(file.getLocation().toOSString());
			config.setInputFile(inputFile);
			config.setMessageRecorder(new ImpToOrcMessageAdapter(markerCreator));
			config.getMessageRecorder().beginProcessing(inputFile);
			file.deleteMarkers(OrcBuilder.PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);

			// TODO: Set options per project settings

			try {
				//XXX: This is horribly hacked -- we're parsing twice, this code is cut-and-pasted, the config set-up is in 3 places, blah, blah.
				//FIXME: Refactor the creating of Config, file reading, compiling phases, etc.
				Expr ex;
				ex = Orc.compile(new InputStreamReader(file.getContents()), config, new SubProgressListener(progress, 0, 0.7));
				if (config.hasOilOutputFile()) {
					final Writer out = config.getOilWriter();
					progress.setNote("Writing OIL");
					new Oil(ex).toXML(out);
					out.close();
				}
				progress.setProgress(0.8);
				if (progress.isCanceled()) {
					return;
				}
				progress.setNote("Creating DAG");
				Compiler.compile(ex, new Pub()); // Disregard DAG result, we just want the errors
				progress.setProgress(0.95);
				if (progress.isCanceled()) {
					return;
				}
			} catch (final CompilationException e) {
				getConsoleStream().println(e.getLocalizedMessage());
				config.getMessageRecorder().recordMessage(Severity.FATAL, 0, e.getMessageOnly(), e.getSourceLocation(), null, e);
			} catch (final IOException e) {
				getConsoleStream().println(e.getLocalizedMessage());
				config.getMessageRecorder().recordMessage(Severity.FATAL, 0, e.getLocalizedMessage(), null, null, e);
			}

			config.getMessageRecorder().endProcessing(inputFile);

			doRefresh(file.getParent()); // N.B.: Assumes all generated files go into parent folder
		} catch (final Exception e) {
			// catch Exception, because any exception could break the
			// builder infrastructure.
			getConsoleStream().println("Compiler internal error on: " + file.getName() + ": " + e.getMessage());
			getPlugin().logException(e.getMessage(), e);
		}
		getConsoleStream().println("Done building Orc file: " + file.getName());
		monitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.BuilderBase#getConsoleName()
	 */
	@Override
	protected String getConsoleName() {
		return "Orc Compiler";
	}
}
