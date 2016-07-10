//
// OrcSourceViewerConfiguration.java -- Java class OrcSourceViewerConfiguration
// Project OrcEclipse
//
// Created by jthywiss on Jul 7, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import edu.utexas.cs.orc.orceclipse.parse.OrcParsingReconciler;
import edu.utexas.cs.orc.orceclipse.parse.OrcTokenScanner;

/**
 * This class unifies the configuration properties of source viewers for orc. An
 * instance of this class are passed to the <code>configure</code> method of
 * <code>ISourceViewer</code>.
 *
 * @see org.eclipse.jface.text.source.ISourceViewer
 * @author jthywiss
 */
public class OrcSourceViewerConfiguration extends TextSourceViewerConfiguration {

    private final OrcEditor orcEditor;

    /**
     * Constructs an object of class OrcSourceViewerConfiguration.
     *
     * @param orcEditor the OrcEditor for this SourceViewer
     */
    public OrcSourceViewerConfiguration(final OrcEditor orcEditor) {
        super(EditorsUI.getPreferenceStore());
        this.orcEditor = orcEditor;
        // FIXME: Orc end-of-line delimiters
    }

    /**
     * Returns all configured content types for the given source viewer. This
     * list tells the caller which content types must be configured for the
     * given source viewer, i.e. for which content types the given source
     * viewer's functionalities must be specified.
     * <p>
     * Currently, the following functions are per-content-type:
     * <ul>
     * <li>Reconciling (optionally)</li>
     * <li>Content formatting (optionally)</li>
     * <li>Auto-editing</li>
     * <li>Mouse double-click responses</li>
     * <li>Indent prefix characters</li>
     * <li>Line prefix characters</li>
     * <li>Text hovers</li>
     * <li>Information providers</li>
     * </ul>
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return the configured content types for the given viewer
     */
    @Override
    public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
        return new String[] { IDocument.DEFAULT_CONTENT_TYPE };
        /*
         * TODO: Add multi-line comment, single-line comment, string literal,
         * ...
         */
    }

    /**
     * Returns the configured partitioning for the given source viewer. The
     * partitioning is used when the querying content types from the source
     * viewer's input document.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return the configured partitioning
     * @see #getConfiguredContentTypes(ISourceViewer)
     */
    @Override
    public String getConfiguredDocumentPartitioning(final ISourceViewer sourceViewer) {
        return IDocumentExtension3.DEFAULT_PARTITIONING;
    }

    /**
     * Returns the reconciler to be used with the given source viewer. A
     * reconciler maps document sections to document partition content types.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return a reconciler or <code>null</code> if reconciling should not be
     *         supported
     */
    @Override
    public IReconciler getReconciler(final ISourceViewer sourceViewer) {
//  if (fPreferenceStore == null || !fPreferenceStore.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED))
//      return null;
//
//  SpellingService spellingService= EditorsUI.getSpellingService();
//  if (spellingService.getActiveSpellingEngineDescriptor(fPreferenceStore) == null)
//      return null;
//
//  IReconcilingStrategy strategy= new SpellingReconcileStrategy(sourceViewer, spellingService);
//  MonoReconciler reconciler= new MonoReconciler(strategy, false);
//  reconciler.setDelay(500);
//  return reconciler;
        final MonoReconciler reconciler = new MonoReconciler(new OrcParsingReconciler(orcEditor), false);
        reconciler.install(sourceViewer);
        return reconciler;
    }

    /**
     * Returns the presentation reconciler to be used with the given source
     * viewer. Presentation reconcilers maps document content to document
     * presentation. This implementation uses token scanning.
     *
     * @param sourceViewer the source viewer
     * @return the presentation reconciler or <code>null</code> if presentation
     *         reconciling should not be supported
     */
    @Override
    public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
        final PresentationReconciler reconciler = new PresentationReconciler();

        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        final DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new OrcTokenScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        return reconciler;
    }

    /**
     * Returns the content formatter to be used with the given source viewer.
     * The formatter formats ranges within documents. The documents are modified
     * by the formatter.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return a content formatter or <code>null</code> if formatting should not
     *         be supported
     */
    @Override
    public IContentFormatter getContentFormatter(final ISourceViewer sourceViewer) {
        // TODO When we decide to support this, set up a MultiPassContentFormatter here
        return super.getContentFormatter(sourceViewer);
    }

    /**
     * Returns the content assistant to be used with the given source viewer. A
     * content assistant provides support on interactive content completion.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return a content assistant or <code>null</code> if content assist should
     *         not be supported
     */
    @Override
    public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
        // TODO When we decide to support this, set up a ContentAssistant here
        return super.getContentAssistant(sourceViewer);
    }

    /**
     * Returns the quick assist assistant to be used with the given source
     * viewer. A quick assist assistant provides support for quick fixes and
     * quick assists.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return a quick assist assistant or <code>null</code> if quick assist
     *         should not be supported
     */
    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(final ISourceViewer sourceViewer) {
        // TODO When we decide to support this, set up a QuickAssistAssistant here
        return super.getQuickAssistAssistant(sourceViewer);
    }

    /**
     * Returns the auto edit strategies to be used with the given source viewer
     * when manipulating text of the given content type. An auto edit strategy
     * intercepts changes that will be applied to a text viewer's document, and
     * can modify them.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @param contentType the content type for which the strategies are
     *            applicable
     * @return the auto edit strategies or <code>null</code> if automatic
     *         editing is not to be enabled
     */
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(final ISourceViewer sourceViewer, final String contentType) {
        // TODO switch on contentType (multi-line comment, etc...)
        return super.getAutoEditStrategies(sourceViewer, contentType);
    }

    /**
     * Returns the default prefixes to be used by the line-prefix operation in
     * the given source viewer for text of the given content type.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @param contentType the content type for which the prefix is applicable
     * @return the default prefixes or <code>null</code> if the prefix operation
     *         should not be supported
     */
    @Override
    public String[] getDefaultPrefixes(final ISourceViewer sourceViewer, final String contentType) {
        return new String[] { "--", "" }; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns the double-click strategy to be used in this viewer when a mouse
     * double-click event is received on text of the given content type.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @param contentType the content type for which the strategy is applicable
     * @return a double-click strategy or <code>null</code> if double clicking
     *         should not be supported
     */
    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
        // TODO switch on contentType, and use our own TextDoubleClickStrategy here
        return super.getDoubleClickStrategy(sourceViewer, contentType);
    }

    /*
     * TextSourceViewer handles all three hover types (vertical ruler, overview
     * ruler, and text) by examining and displaying the annotations. No need to
     * create our own hovers, just create annotations. By setting text editor
     * preferences, the user can configure which annotations appear.
     */

    /**
     * Returns the information presenter used in response to the source editor's
     * "Show Information" (also known as "Show Tooltip Description") command. An
     * information presenter shows information available at the text viewer's
     * current document position. An information presenter has a list of
     * information providers which return the information to present.
     *
     * @param sourceViewer the source viewer to be configured by this
     *            configuration
     * @return an information presenter <code>null</code> if no information
     *         presenter should be installed
     */
    @Override
    public IInformationPresenter getInformationPresenter(final ISourceViewer sourceViewer) {
        final InformationPresenter presenter = new InformationPresenter(new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(final Shell parent) {
                return new DefaultInformationControl(parent, true);
            }
        });
        presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        // Register information provider
        final IInformationProvider provider = new OrcInformationProvider();
        final String[] contentTypes = getConfiguredContentTypes(sourceViewer);
        for (final String contentType : contentTypes) {
            presenter.setInformationProvider(provider, contentType);
        }

        // sizes: see org.eclipse.jface.text.TextViewer.TEXT_HOVER_*_CHARS
        presenter.setSizeConstraints(100, 12, false, true);
        return presenter;
    }

    /*
     * If we decide to support hyperlink detection, contribute the detectors by
     * the <code>org.eclipse.ui.workbench.texteditor.hyperlinkDetectors</code>
     * extension point in <code>plugin.xml</code>.
     */

}
