//
// RootEvent.java -- Java class RootEvent
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.events;

/**
 * The root event is like a ForkEvent but
 * it is its own thread.
 * @author quark
 */
public class RootEvent extends ForkEvent {
	@Override
	public String getType() {
		return "root";
	}

	@Override
	public ForkEvent getThread() {
		// Since Handles can't serialize circular
		// references, yet, we have to fake the
		// circular reference here.
		return this;
	}
}
