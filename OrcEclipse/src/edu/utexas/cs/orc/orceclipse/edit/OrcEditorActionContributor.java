//
// OrcEditorActionContributor.java -- Java class OrcEditorActionContributor
// Project OrcEclipse
//
// Created by jthywiss on Jul 10, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.util.ResourceBundle;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

/**
 * Manages the installation and removal of global actions for Orc editors.
 *
 * @see IEditorActionBarContributor
 * @author jthywiss
 */
public class OrcEditorActionContributor extends TextEditorActionContributor {

    private final RetargetTextEditorAction toggleInsertModeAction;

    /**
     * Constructs an object of class OrcEditorActionContributor.
     */
    public OrcEditorActionContributor() {
        super();

        final ResourceBundle resourceBundle = ResourceBundle.getBundle("edu.utexas.cs.orc.orceclipse.messages"); //$NON-NLS-1$

        toggleInsertModeAction = new RetargetTextEditorAction(resourceBundle, "Editor.ToggleInsertMode.", IAction.AS_CHECK_BOX); //$NON-NLS-1$
        toggleInsertModeAction.setActionDefinitionId(ITextEditorActionDefinitionIds.TOGGLE_INSERT_MODE);
    }

    /**
     * Initializes this contributor, which is expected to add contributions as
     * required to the given action bars and global action handlers.
     * <p>
     * The page is passed to support the use of <code>RetargetAction</code> by
     * the contributor. In this case the init method implementors should:
     * </p>
     * <p>
     * <ol>
     * <li>Set retarget actions as global action handlers.</li>
     * <li>Add the retarget actions as part listeners.</li>
     * <li>Get the active part and if not <code>null</code> call partActivated
     * on the retarget actions.</li>
     * </ol>
     * </p>
     * <p>
     * And in the dispose method the retarget actions should be removed as part
     * listeners.
     * </p>
     *
     * @param bars the action bars
     * @param page the workbench page for this contributor
     */
    @Override
    public void init(final IActionBars bars, final IWorkbenchPage page) {
        super.init(bars, page);
    }

    /**
     * Sets the active editor for the contributor. Implementors should
     * disconnect from the old editor, connect to the new editor, and update the
     * actions to reflect the new editor.
     *
     * @param targetEditor the new editor target
     */
    @Override
    public void setActiveEditor(final IEditorPart targetEditor) {
        super.setActiveEditor(targetEditor);
        orcSetActiveEditor(targetEditor);
    }

    private void orcSetActiveEditor(final IEditorPart targetEditor) {
        if (targetEditor instanceof ITextEditor) {
            final ITextEditor targetTextEditor = (ITextEditor) targetEditor;
            toggleInsertModeAction.setAction(getAction(targetTextEditor, ITextEditorActionConstants.TOGGLE_INSERT_MODE));
        }
    }

    /**
     * Disposes this contributor.
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Contribute actions to an editor menu bar and sub-menus.
     *
     * @param menuManager menu manager for an editor
     */
    @Override
    public void contributeToMenu(final IMenuManager menuManager) {
        super.contributeToMenu(menuManager);

        /* Add Edit -> Smart Insert Mode */
        final IMenuManager editMenu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, toggleInsertModeAction);
        }
    }

    /**
     * Contribute actions to an editor tool bar.
     *
     * @param toolBarManager tool bar manager for an editor
     */
    @Override
    public void contributeToToolBar(final IToolBarManager toolBarManager) {
        super.contributeToToolBar(toolBarManager);
    }

    /**
     * Contribute actions to an editor cool bar.
     *
     * @param coolBarManager cool bar manager for an editor
     */
    @Override
    public void contributeToCoolBar(final ICoolBarManager coolBarManager) {
        super.contributeToCoolBar(coolBarManager);
    }

    /**
     * Contribute actions to an editor status line.
     *
     * @param statusLineManager status line manager for an editor
     */
    @Override
    public void contributeToStatusLine(final IStatusLineManager statusLineManager) {
        super.contributeToStatusLine(statusLineManager);
    }

}
