//
// ImpToOrcProgressAdapter.java -- Java class ImpToOrcProgressAdapter
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import orc.progress.ProgressMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Wraps Eclipse's IProgressMonitor interface in Orc's ProgressListener
 * interface.
 *
 * @author jthywiss
 */
public class ImpToOrcProgressAdapter implements ProgressMonitor {
	private final SubMonitor prgrsMntr;

	/**
	 * Constructs an object of class ImpToOrcProgressAdapter.
	 *
	 * @param monitor the IProgressMonitor to wrap
	 */
	public ImpToOrcProgressAdapter(final IProgressMonitor monitor) {
		prgrsMntr = SubMonitor.convert(monitor);
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#setTaskName(java.lang.String)
	 */
	@Override
	public void setTaskName(final String name) {
		prgrsMntr.setTaskName(name);
		prgrsMntr.subTask(name);
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#setWorkRemaining(int)
	 */
	@Override
	public void setWorkRemaining(final int remainWorkQty) {
		prgrsMntr.setWorkRemaining(remainWorkQty);
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#setIndeterminate()
	 */
	@Override
	public void setIndeterminate() {
		// Can't do this in a SubMonitor (IProgressMonitor.UNKNOWN will cause problems)
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#worked(int)
	 */
	@Override
	public void worked(final int completedWorkIncrement) {
		prgrsMntr.worked(completedWorkIncrement);
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#newChild(int)
	 */
	@Override
	public ProgressMonitor newChild(final int delegatedWorkQty) {
		return new ImpToOrcProgressAdapter(prgrsMntr.newChild(delegatedWorkQty, 0));
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#isCanceled()
	 */
	@Override
	public boolean isCanceled() {
		return prgrsMntr.isCanceled();
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#setBlocked(java.lang.String)
	 */
	@Override
	public void setBlocked(final String reason) {
		prgrsMntr.setBlocked(new Status(IStatus.INFO, Activator.getInstance().getID(), reason));
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressMonitor#clearBlocked()
	 */
	@Override
	public void clearBlocked() {
		prgrsMntr.clearBlocked();
	}

}
