//
// ProgressListener.java -- Java interface ProgressListener
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
 * Generic interface for things which track the progress
 * of tasks.
 * @author quark
 */
public interface ProgressListener {
	public boolean isCanceled();

	public void setNote(final String note);

	public void setProgress(double v);
}
