//
// OrcEditor.java -- Java class OrcEditor
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import orc.ast.AST;

import edu.utexas.cs.orc.orceclipse.ActionStrings;
import edu.utexas.cs.orc.orceclipse.OrcPluginIds;
import edu.utexas.cs.orc.orceclipse.edit.OrcContentProvider.OutlineTreeAstNode;
import edu.utexas.cs.orc.orceclipse.edit.OrcContentProvider.OutlineTreeFileNode;
import edu.utexas.cs.orc.orceclipse.edit.OrcContentProvider.OutlineTreeNode;

/**
 * An Orc source code text editor.
 *
 * @author jthywiss
 */
public class OrcEditor extends TextEditor {

    private OrcContentOutlinePage contentOutlinePage;

    /**
     * Constructs an object of class OrcEditor.
     */
    public OrcEditor() {
        super();
        setDocumentProvider(new OrcDocumentProvider());

        // fAnnotationPreferences=
        // EditorsPlugin.getDefault().getMarkerAnnotationPreferences();
        // setRangeIndicator(new DefaultRangeIndicator());

    }

    /**
     * Disposes of this TextEditor.
     * <p>
     * This is the last method called on the <code>TextEditor</code>.
     * </p>
     * <p>
     * Release any resources, fonts, images, etc.&nbsp; held by this TextEditor.
     * It is also very important to deregister all listeners from the workbench.
     * </p>
     * <p>
     * Clients should not call this method (the workbench calls this method at
     * appropriate times).
     * </p>
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Configures this editor.
     */
    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        setSourceViewerConfiguration(new OrcSourceViewerConfiguration(this));
        setEditorContextMenuId(OrcPluginIds.ContributionItems.ORC_EDITOR_CONTEXT_MENU);
        setRulerContextMenuId(OrcPluginIds.ContributionItems.ORC_EDITOR_RULER_CONTEXT_MENU);

        /* Enable SMART_INSERT with our autoEditStrategies */
        configureInsertMode(SMART_INSERT, true);
        configureInsertMode(INSERT, false);
        configureInsertMode(INSERT, true);
        setInsertMode(SMART_INSERT);
    }

    /**
     * Configures the decoration support for this editor's source viewer.
     *
     * @param support the decoration support to configure
     */
    @Override
    protected void configureSourceViewerDecorationSupport(final SourceViewerDecorationSupport support) {
        final DefaultCharacterPairMatcher charPairMatcher = new DefaultCharacterPairMatcher(new char[] { '(', ')', '{', '}', '[', ']' }, IDocumentExtension3.DEFAULT_PARTITIONING);
        support.setCharacterPairMatcher(charPairMatcher);
        /* Use preferences from JDT editors */
        support.setMatchingCharacterPainterPreferenceKeys(PreferenceConstants.EDITOR_MATCHING_BRACKETS, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, PreferenceConstants.EDITOR_HIGHLIGHT_BRACKET_AT_CARET_LOCATION, PreferenceConstants.EDITOR_ENCLOSING_BRACKETS);

        super.configureSourceViewerDecorationSupport(support);
    }

    /**
     * Initializes the key binding scopes of this editor.
     */
    @Override
    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { OrcPluginIds.Context.ORC_EDITOR_SCOPE });
    }

    /**
     * Creates this editor's standard actions and connects them with the global
     * workbench actions.
     */
    @Override
    protected void createActions() {
        super.createActions();

        {
            final TextOperationAction action = new TextOperationAction(ActionStrings.getResourceBundle(), ActionStrings.COMMENT, this, ITextOperationTarget.PREFIX);
            action.setActionDefinitionId(OrcPluginIds.Command.COMMENT);
            setAction(OrcPluginIds.Action.COMMENT, action);
            markAsStateDependentAction(OrcPluginIds.Action.COMMENT, true);
        }
        {
            final TextOperationAction action = new TextOperationAction(ActionStrings.getResourceBundle(), ActionStrings.UNCOMMENT, this, ITextOperationTarget.STRIP_PREFIX);
            action.setActionDefinitionId(OrcPluginIds.Command.UNCOMMENT);
            setAction(OrcPluginIds.Action.UNCOMMENT, action);
            markAsStateDependentAction(OrcPluginIds.Action.UNCOMMENT, true);
        }
    }

    /**
     * Sets up this editor's context menu before it is made visible.
     */
    @Override
    protected void editorContextMenuAboutToShow(final IMenuManager menu) {
        super.editorContextMenuAboutToShow(menu);
    }

    /**
     * Returns an object which is an instance of the given class associated with
     * this object. Returns <code>null</code> if no such object can be found.
     *
     * @param adapter the adapter class to look up
     * @return a object castable to the given class, or <code>null</code> if
     *         this object does not have an adapter for the given class
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
        if (IContentOutlinePage.class.equals(adapter)) {
            return getContentOutlinePage();
        }

        return super.getAdapter(adapter);
    }

    /**
     * @return
     */
    protected OrcContentOutlinePage getContentOutlinePage() {
        if (contentOutlinePage == null) {
            contentOutlinePage = new OrcContentOutlinePage(this);
        }
        return contentOutlinePage;
    }

    /**
     * Called before reconciling is started.
     *
     * @param document the document that will be reconciled
     * @param progressMonitor the progress monitor
     */
    public void aboutToBeReconciled(final IDocument document, final IProgressMonitor progressMonitor) {
        /* Nothing needed */
    }

    /**
     * Called after reconciling has been finished.
     *
     * @param ast the parsed AST, or <code>null</code> if the parsing failed or
     *            was cancelled
     * @param document the document that was reconciled
     * @param progressMonitor the progress monitor
     */
    public void reconciled(final AST ast, final IDocument document, final IProgressMonitor progressMonitor) {
        final OutlineTreeNode outlineTree = new OutlineTreeFileNode(null, ((IFileEditorInput) getEditorInput()).getFile());
        OutlineTreeAstNode.addSubtree(outlineTree, ast);
        Display.getDefault().asyncExec(() -> getContentOutlinePage().setOutlineRoot(outlineTree));
    }

}
