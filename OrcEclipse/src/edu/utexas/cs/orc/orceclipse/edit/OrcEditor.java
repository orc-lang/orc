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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import orc.ast.AST;

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
        setEditorContextMenuId("#OrcEditorContext"); //$NON-NLS-1$
        setRulerContextMenuId("#OrcRulerContext"); //$NON-NLS-1$
        setHelpContextId("edu.utexas.cs.orc.orceclipse.text_editor_context"); //$NON-NLS-1$
        configureInsertMode(SMART_INSERT, true);
        setInsertMode(SMART_INSERT);
    }

    /**
     * Initializes the key binding scopes of this editor.
     */
    @Override
    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { "edu.utexas.cs.orc.orceclipse.orcEditorScope" }); //$NON-NLS-1$
    }

    /**
     * Creates this editor's standard actions and connects them with the global
     * workbench actions.
     */
    @Override
    protected void createActions() {
        // TODO Auto-generated method stub
        super.createActions();
    }

    /**
     * Sets up this editor's context menu before it is made visible.
     */
    @Override
    protected void editorContextMenuAboutToShow(final IMenuManager menu) {
        // TODO Auto-generated method stub
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
