//
// OrcRuntimeClasspathTab.java -- Java class OrcRuntimeClasspathTab
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Feb 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;

import edu.utexas.cs.orc.orceclipse.Messages;

/**
 *
 *
 * @author jthywiss
 */
public class OrcRuntimeClasspathTab extends JavaClasspathTab {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab#getName()
	 */
	@Override
	public String getName() {
		return Messages.OrcRuntimeClasspathTab_RuntimeClasspathTabName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab#getId()
	 */
	@Override
	public String getId() {
		return "edu.utexas.cs.orc.orceclipse.launch.OrcRuntimeClasspathTab"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab#isShowBootpath()
	 */
	@Override
	public boolean isShowBootpath() {
		return false;
	}

}
