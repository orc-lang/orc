//
// ProgressMonitorListener.java -- Java class ProgressMonitorListener
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.progress;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ProgressMonitor;

/**
 * A ProgressListener which runs a ProgressMonitor to display progress.
 * @author quark
 */
public final class ProgressMonitorListener implements ProgressListener {
	private final ProgressMonitor progress;
	private final AtomicBoolean isCanceled = new AtomicBoolean(false);

	public ProgressMonitorListener(final Component parent, final Object message, final String note) {
		progress = new ProgressMonitor(parent, message, note, 0, 100);
	}

	public boolean isCanceled() {
		return isCanceled.get();
	}

	public void setNote(final String note) {
		invokeLater(new Runnable() {
			public void run() {
				if (progress.isCanceled()) {
					isCanceled.set(true);
				}
				progress.setNote(note);
			}
		});
	}

	public void setProgress(final double v) {
		invokeLater(new Runnable() {
			public void run() {
				if (progress.isCanceled()) {
					isCanceled.set(true);
				}
				progress.setProgress((int) (v * 100));
			}
		});
	}
}
