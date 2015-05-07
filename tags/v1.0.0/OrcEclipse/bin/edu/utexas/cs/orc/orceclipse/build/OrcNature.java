//
// OrcNature.java -- Java class OrcNature
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

package edu.utexas.cs.orc.orceclipse.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.imp.builder.ProjectNatureBase;
import org.eclipse.imp.runtime.IPluginLog;

import edu.utexas.cs.orc.orceclipse.Activator;

/**
 * Orc project nature. When a project is configured with the 
 * Orc nature, it will have the Orc builder,
 * Orc project actions, and so on.
 *
 * @see org.eclipse.core.resources.IProjectNature
 */
public class OrcNature extends ProjectNatureBase {
	private static final String natureID = Activator.getInstance().getID() + ".project.orcNature"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.ProjectNatureBase#getNatureID()
	 */
	@Override
	public String getNatureID() {
		return natureID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.ProjectNatureBase#getBuilderID()
	 */
	@Override
	public String getBuilderID() {
		return OrcBuilder.BUILDER_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.ProjectNatureBase#addToProject(org.eclipse.core.resources.IProject)
	 */
	@Override
	public void addToProject(final IProject project) {
		super.addToProject(project);
	};

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.ProjectNatureBase#refreshPrefs()
	 */
	@Override
	protected void refreshPrefs() {
		// Nothing needed here presently
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.ProjectNatureBase#getLog()
	 */
	@Override
	public IPluginLog getLog() {
		return Activator.getInstance();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.builder.ProjectNatureBase#getDownstreamBuilderID()
	 */
	@Override
	protected String getDownstreamBuilderID() {
		return null;
	}
}
