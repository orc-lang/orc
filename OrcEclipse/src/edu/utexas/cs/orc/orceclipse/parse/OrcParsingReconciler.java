//
// OrcParsingReconciler.java -- Java class OrcParsingReconciler
// Project OrcEclipse
//
// Created by jthywiss on Jul 8, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.parse;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;

import scala.util.parsing.combinator.Parsers.NoSuccess;
import scala.util.parsing.combinator.Parsers.ParseResult;

import orc.OrcCompilationOptions;
import orc.ast.AST;
import orc.compile.CompilerOptions;
import orc.compile.parse.OrcIncludeParser;
import orc.compile.parse.OrcInputContext;
import orc.compile.parse.OrcProgramParser;
import orc.compile.parse.OrcStringInputContext;
import orc.error.compiletime.CompileLogger.Severity;
import orc.error.compiletime.ParsingException;

import edu.utexas.cs.orc.orceclipse.EclipseToOrcMessageAdapter;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;
import edu.utexas.cs.orc.orceclipse.OrcPlugin;
import edu.utexas.cs.orc.orceclipse.edit.OrcEditor;

/**
 * An Eclipse JFace "reconciling strategy" that parses an Orc source document
 * and notifies its associated OrcEditor of the parse results (AST). As a
 * side-effect of parsing, parse problem markers are added to the editor input
 * resource.
 *
 * @author jthywiss
 */
public class OrcParsingReconciler implements IReconcilingStrategy, IReconcilingStrategyExtension {

    /** source ID value to use on problem markers */
    public static final String SOURCE_ID = OrcParsingReconciler.class.getName();

    private final OrcEditor orcEditor;
    private IDocument document;
    private IProgressMonitor progressMonitor;

    /**
     * Constructs an object of class OrcParsingReconciler.
     *
     * @param orcEditor the OrcEditor this reconciler is working on behalf of
     */
    public OrcParsingReconciler(final OrcEditor orcEditor) {
        this.orcEditor = orcEditor;
    }

    @Override
    public void setDocument(final IDocument document) {
        this.document = document;
    }

    @Override
    public void setProgressMonitor(final IProgressMonitor monitor) {
        progressMonitor = monitor;
    }

    @Override
    public void initialReconcile() {
        orcEditor.aboutToBeReconciled(document, progressMonitor);
        final AST ast = parse();
        orcEditor.reconciled(ast, document, progressMonitor);
    }

    @Override
    public void reconcile(final DirtyRegion dirtyRegion, final IRegion subRegion) {
        orcEditor.aboutToBeReconciled(document, progressMonitor);
        final AST ast = parse();
        orcEditor.reconciled(ast, document, progressMonitor);
    }

    @Override
    public void reconcile(final IRegion partition) {
        orcEditor.aboutToBeReconciled(document, progressMonitor);
        final AST ast = parse();
        orcEditor.reconciled(ast, document, progressMonitor);
    }

    protected final boolean isCanceled() {
        return progressMonitor != null && progressMonitor.isCanceled();
    }

    /**
     * Parse the given source and return the resulting AST.
     * <p>
     * {@link #setDocument(IDocument)} and
     * {@link #setProgressMonitor(IProgressMonitor)} must be called before
     * calling this method.
     *
     * @return the AST, if any, resulting from the parse
     */
    protected AST parse() {

        if (isCanceled()) {
            return null;
        }

        final String inputName = orcEditor.getEditorInput().getName();
        final IPath inputAbsolutePath = ((IPathEditorInput) orcEditor.getEditorInput()).getPath();
        final IFile inputFile = ((IFileEditorInput) orcEditor.getEditorInput()).getFile();
        final IProject inputProject = inputFile.getProject();

        OrcConfigSettings config;
        try {
            config = new OrcConfigSettings(inputProject, null);
        } catch (final CoreException e) {
            // Shouldn't happen with project settings only
            OrcPlugin.logAndShow(e);
            return null;
        }

        final OrcStringInputContext ic = new OrcStringInputContext(document.get()) {
            @Override
            public String descr() {
                return inputName;
            }

            @Override
            public URI toURI() {
                return inputAbsolutePath.toFile().toURI();
            }
        };

        if (isCanceled()) {
            return null;
        }

        final EclipseToOrcMessageAdapter compileLogger = new EclipseToOrcMessageAdapter(SOURCE_ID, true);
        final CompilerOptions co = new CompilerOptions(config, compileLogger);

        compileLogger.beginProcessing(ic);

        final orc.OrcCompilerRequires dummyEnvServices = new orc.OrcCompilerRequires() {
            @Override
            public OrcInputContext openInclude(final String includeName, final OrcInputContext orcinputcontext, final OrcCompilationOptions orcoptions) {
                return new OrcStringInputContext("") { //$NON-NLS-1$
                    @Override
                    public String descr() {
                        return includeName;
                    }
                };
            }

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public Class loadClass(final String s) {
                return null;
            }
        };

        try {
            ParseResult<?> result;
            if (!OrcPlugin.isOrcIncludeFile(inputFile)) {
                result = OrcProgramParser.apply(ic, co, dummyEnvServices);
            } else {
                result = OrcIncludeParser.apply(ic, co, dummyEnvServices);
            }
            if (result.successful()) {
                return (AST) result.get();
            } else {
                final NoSuccess n = (NoSuccess) result;
                compileLogger.recordMessage(Severity.FATAL, 0, n.msg(), n.next().pos(), null, new ParsingException(n.msg(), n.next().pos()));
                return null;
            }
        } catch (final Exception e) {
            compileLogger.recordMessage(Severity.FATAL, 0, e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getCanonicalName(), null, null, e);
            return null;
        } finally {
            compileLogger.endProcessing(ic);
        }

    }

}
