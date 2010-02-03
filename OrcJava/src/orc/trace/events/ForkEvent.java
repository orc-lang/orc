//
// ForkEvent.java -- Java class ForkEvent
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
 * Spawning a new thread.
 * @author quark
 */
public class ForkEvent extends Event {
	@Override
	public String getType() {
		return "fork";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
