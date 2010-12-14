//
// OrcNewProjectWizard.java -- Java class OrcNewProjectWizard
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 20, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.project;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.build.OrcNature;

/**
 * Adds only one aspect to the BasicNewProjectResourceWizard --
 * add the Orc nature to the project.
 *
 * @author jthywiss
 */
public class OrcNewProjectWizard extends BasicNewProjectResourceWizard {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		final boolean finishAccepted = super.performFinish();
		if (getNewProject() != null) {
			new OrcNature().addToProject(getNewProject());
			try {
				getNewProject().setDefaultCharset("UTF-8", new NullProgressMonitor()); //$NON-NLS-1$
			} catch (final CoreException e) {
				Activator.logAndShow(e);
			}
		}
		return finishAccepted;
	}

}
