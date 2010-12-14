//
// NewOrcFileWizard.java -- Java class NewOrcFileWizard
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Feb 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import edu.utexas.cs.orc.orceclipse.Activator;

/**
 * UI "wizard" to creates a new Orc file.  It creates a file in the
 * currently selected container (directory).  It attempts to open the
 * new file in the editor defined for the .orc file type.
 *
 * @author jthywiss
 */
public class NewOrcFileWizard extends Wizard implements INewWizard {
	private NewOrcFileWizardPage page;
	private IStructuredSelection selection;
	private IWorkbench workbench;

	/**
	 * Constructs an object of class NewOrcFileWizard.
	 *
	 */
	public NewOrcFileWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Initializes this creation wizard using the passed workbench and
	 * object selection.
	 * <p>
	 * This method is called after the no argument constructor and
	 * before other methods are called.
	 *
	 * @param workbench the current workbench
	 * @param selection the current object selection
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	/**
	 * Adds NewOrcFileWizardPage to the Wizard
	 */
	@Override
	public void addPages() {
		page = new NewOrcFileWizardPage("newFilePage1", selection); //$NON-NLS-1$
		addPage(page);
	}

	/**
	 * Called when 'Finish' button is pressed in the wizard.  Creates
	 * the file, and opens an editor on it. 
	 *
	 * @return <code>true</code> to indicate the finish request
	 *   was accepted, and <code>false</code> to indicate
	 *   that the finish request was refused
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		final IFile file = page.createNewFile();
		if (file == null) {
			return false;
		}
		try {
			file.setCharset("UTF-8", new NullProgressMonitor()); //$NON-NLS-1$
		} catch (final CoreException e) {
			Activator.logAndShow(e);
		}

		selectAndReveal(file);

		// Open editor on new file.
		final IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				final IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
		} catch (final PartInitException e) {
			Activator.logAndShow(e);
		}

		return true;
	}

	/**
	 * Selects and reveals the newly added resource in all parts
	 * of the active workbench window's active page.
	 *
	 * @param newResource Resource to be revealed
	 * @see ISetSelectionTarget
	 */
	protected void selectAndReveal(final IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, workbench.getActiveWorkbenchWindow());
	}

}
