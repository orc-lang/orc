//
// PullEvent.java -- Java class PullEvent
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

import orc.trace.TokenTracer.PullTrace;

/**
 * This is just a way to uniquely identify a pull. It should preceed the
 * corresponding fork event.
 * 
 * @author quark
 */
public class PullEvent extends Event implements PullTrace {
	@Override
	public String getType() {
		return "pull";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
