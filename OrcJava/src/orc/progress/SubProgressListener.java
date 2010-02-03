//
// SubProgressListener.java -- Java class SubProgressListener
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

/**
 * Report progress of a subtask to a progress listener for a larger task.
 * @author quark
 */
public final class SubProgressListener implements ProgressListener {
	private final ProgressListener parent;
	private final double min;
	private final double max;

	/**
	 * min and max represent the progress range tracked by this listener
	 * within the larger task tracked by parent.
	 */
	public SubProgressListener(final ProgressListener parent, final double min, final double max) {
		assert min < max;
		this.parent = parent;
		this.min = min;
		this.max = max;
	}

	public boolean isCanceled() {
		return parent.isCanceled();
	}

	public void setNote(final String note) {
		parent.setNote(note);
	}

	/**
	 * Progress reported here is interpreted as the percentage of
	 * the subtask complete, which we translate into the percentage
	 * of the overall task complete.
	 */
	public void setProgress(final double v) {
		parent.setProgress(min + v * (max - min));
	}
}
