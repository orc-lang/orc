//
// NullProgressListener.java -- Java class NullProgressListener
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
 * Progress listener which does nothing.
 * @author quark
 */
public final class NullProgressListener implements ProgressListener {
	public final static NullProgressListener singleton = new NullProgressListener();

	private NullProgressListener() {
	}

	public boolean isCanceled() {
		return false;
	}

	public void setNote(final String note) {
		// do nothing
	}

	public void setProgress(final double v) {
		// do nothing
	}
}
