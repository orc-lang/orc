//
// EclipseToOrcProgressAdapter.java -- Java class EclipseToOrcProgressAdapter
// Project OrcEclipse
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import orc.progress.ProgressMonitor;

/**
 * Wraps Eclipse's IProgressMonitor interface in Orc's ProgressListener
 * interface.
 *
 * @author jthywiss
 */
public class EclipseToOrcProgressAdapter implements ProgressMonitor {
    private final SubMonitor prgrsMntr;

    /**
     * Constructs an object of class EclipseToOrcProgressAdapter.
     *
     * @param monitor the IProgressMonitor to wrap
     */
    public EclipseToOrcProgressAdapter(final IProgressMonitor monitor) {
        prgrsMntr = SubMonitor.convert(monitor);
    }

    @Override
    public void setTaskName(final String name) {
        prgrsMntr.setTaskName(name);
        prgrsMntr.subTask(name);
    }

    @Override
    public void setWorkRemaining(final int remainWorkQty) {
        prgrsMntr.setWorkRemaining(remainWorkQty);
    }

    @Override
    public void setIndeterminate() {
        /*
         * Can't do this in a SubMonitor (IProgressMonitor.UNKNOWN will cause
         * problems)
         */
    }

    @Override
    public void worked(final int completedWorkIncrement) {
        prgrsMntr.worked(completedWorkIncrement);
    }

    @Override
    public ProgressMonitor newChild(final int delegatedWorkQty) {
        return new EclipseToOrcProgressAdapter(prgrsMntr.newChild(delegatedWorkQty, 0));
    }

    @Override
    public boolean isCanceled() {
        return prgrsMntr.isCanceled();
    }

    @Override
    public void setBlocked(final String reason) {
        prgrsMntr.setBlocked(new Status(IStatus.INFO, OrcPlugin.getId(), reason));
    }

    @Override
    public void clearBlocked() {
        prgrsMntr.clearBlocked();
    }

}
