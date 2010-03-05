//
// DieEvent.java -- Java class DieEvent
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

import orc.trace.handles.LastHandle;

/**
 * Always the last event in a thread.
 * @author quark
 */
public class DieEvent extends Event {
	@Override
	public void setThread(final ForkEvent thread) {
		this.thread = new LastHandle<ForkEvent>(thread);
	}

	@Override
	public String getType() {
		return "die";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
