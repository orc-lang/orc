//
// EnableOrcNature.java -- Java class EnableOrcNature
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

package edu.utexas.cs.orc.orceclipse.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.utexas.cs.orc.orceclipse.build.OrcNature;

/**
 * Adds an Orc "nature" to the selected project's attributes.
 * <p>
 * (This class is a UI action delegate. The action is defined in
 * the <code>plugin.xml</code> file.)
 */
public class EnableOrcNature implements IWorkbenchWindowActionDelegate {
	private IProject fProject;

	/**
	 * Constructs an object of class EnableOrcNature.
	 *
	 */
	public EnableOrcNature() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(final IWorkbenchWindow window) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(final IAction action) {
		new OrcNature().addToProject(fProject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(final IAction action, final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ss = (IStructuredSelection) selection;
			final Object first = ss.getFirstElement();

			if (first instanceof IProject) {
				fProject = (IProject) first;
			} else if (first instanceof IJavaProject) {
				fProject = ((IJavaProject) first).getProject();
			}
		}
	}
}
