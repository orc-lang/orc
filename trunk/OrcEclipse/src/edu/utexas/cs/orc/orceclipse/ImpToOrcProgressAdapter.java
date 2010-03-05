//
// ImpToOrcProgressAdapter.java -- Java class ImpToOrcProgressAdapter
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import orc.progress.ProgressListener;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Wraps IMP's IProgressMonitor interface in Orc's ProgressListener
 * interface.
 *
 * @author jthywiss
 */
public class ImpToOrcProgressAdapter implements ProgressListener {
	private final IProgressMonitor impProgressMonitor;

	/**
	 * Constructs an object of class ImpToOrcProgressAdapter.
	 *
	 * @param monitor the IProgressMonitor to wrap
	 */
	public ImpToOrcProgressAdapter(final IProgressMonitor monitor) {
		this.impProgressMonitor = monitor;
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressListener#isCanceled()
	 */
	public boolean isCanceled() {
		return impProgressMonitor.isCanceled();
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressListener#setNote(java.lang.String)
	 */
	public void setNote(final String note) {
		impProgressMonitor.subTask(note);
	}

	/* (non-Javadoc)
	 * @see orc.progress.ProgressListener#setProgress(double)
	 */
	public void setProgress(final double v) {
		impProgressMonitor.worked((int) (100 * v));
		if (v > 0.99) { // "Close enough" :-)
			impProgressMonitor.done();
		}
	}

}
