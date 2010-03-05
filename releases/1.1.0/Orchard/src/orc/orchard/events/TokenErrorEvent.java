//
// TokenErrorEvent.java -- Java class TokenErrorEvent
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

import orc.error.SourceLocation;
import orc.error.runtime.TokenException;

public class TokenErrorEvent extends JobEvent {
	public String message;
	public SourceLocation location;

	public TokenErrorEvent() {
	}

	public TokenErrorEvent(final TokenException problem) {
		location = problem.getSourceLocation();
		message = problem.getMessage();
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
