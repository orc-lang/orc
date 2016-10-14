//
// OrcContentOutlinePage.java -- Java class OrcContentOutlinePage
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

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import edu.utexas.cs.orc.orceclipse.edit.OrcContentProvider.OutlineTreeNode;

/**
 * The Orc editor's content outline page. This content outline page will be
 * presented to the user via the standard Content Outline View (the user decides
 * whether their workbench window contains this view) whenever that editor is
 * active. </p>
 * <p>
 * Internally, each content outline page consists of a standard tree viewer;
 * selections made in the tree viewer are reported as selection change events by
 * the page (which is a selection provider). The tree viewer is not created
 * until <code>createPage</code> is called; consequently, subclasses must extend
 * <code>createControl</code> to configure the tree viewer with a proper content
 * provider, label provider, and input element.
 * </p>
 * <p>
 * Access to a content outline page begins when an editor is activated. When
 * activation occurs, the content outline view will ask the editor for its
 * content outline page. This is done by invoking
 * <code>getAdapter(IContentOutlinePage.class)</code> on the editor. If the
 * editor returns a page, the view then creates the controls for that page
 * (using <code>createControl</code>) and makes the page visible.
 * </p>
 *
 * @author jthywiss
 */
public class OrcContentOutlinePage extends ContentOutlinePage {

    private final OrcEditor pairedEditor;

    /**
     * Constructs an object of class OrcContentOutlinePage.
     *
     * @param editor the OrcEditor instance this outline page reflects
     */
    public OrcContentOutlinePage(final OrcEditor editor) {
        pairedEditor = editor;
    }

    /**
     * Creates the SWT control for this page under the given parent control.
     * Here, we configure the superclass-provided tree viewer with a content
     * provider, label provider, and input element.
     *
     * @param parent the parent control
     */
    @Override
    public void createControl(final Composite parent) {

        super.createControl(parent);

        final TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new OrcContentProvider());
        viewer.setLabelProvider(new OrcLabelProvider());

        viewer.setAutoExpandLevel(2);
    }

    /**
     * Disposes of this page.
     * <p>
     * This is the last method called on the <code>IPage</code>. Implementors
     * should clean up any resources associated with the page.
     * </p>
     * Callers of this method should ensure that the page's control (if it
     * exists) has been disposed before calling this method. However, for
     * backward compatibilty, implementors must also ensure that the page's
     * control has been disposed before this method returns. </p>
     * <p>
     * Note that there is no guarantee that createControl() has been called, so
     * the control may never have been created.
     * </p>
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Notifies that the selection has changed.
     *
     * @param event event object describing the change
     */
    @Override
    public void selectionChanged(final SelectionChangedEvent event) {
        super.selectionChanged(event);

        final ITreeSelection selection = (ITreeSelection) event.getSelection();
        if (selection.isEmpty()) {
            return;
        }
        final OutlineTreeNode selection1 = (OutlineTreeNode) selection.getFirstElement();
        if (selection1 instanceof IRegion) {
            final IRegion region = (IRegion) selection1;
            pairedEditor.selectAndReveal(region.getOffset(), region.getLength());
        } else {
            pairedEditor.resetHighlightRange();
        }
    }

    /**
     * @param root the new root OutlineTreeNode to be displayed in the outline
     *            view
     */
    public void setOutlineRoot(final OutlineTreeNode root) {
        getTreeViewer().setInput(root);
    }

}
