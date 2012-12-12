//
// TokenErrorEvent.java -- Java class TokenErrorEvent
// Project Orchard
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

import orc.error.OrcException;
import orc.error.runtime.JavaException;
import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.Position;
import scala.util.parsing.input.Positional;

public class TokenErrorEvent extends JobEvent {
	public String message;
	public String positionString;

	public TokenErrorEvent() {
	}

	public TokenErrorEvent(final Throwable problem) {
		if (problem instanceof Positional) {
			Position pos = ((Positional) problem).pos();
			if (pos != null && !(pos instanceof NoPosition$)) {
				positionString = pos.toString();
			}
		}
		if (problem instanceof JavaException) {
			JavaException je = (JavaException)problem;
			message = je.getMessageAndPositon() + "\n" + je.getOrcStacktraceAsString();
		} else if (problem instanceof OrcException) {
			OrcException oe = (OrcException)problem;
			message = oe.getMessageAndDiagnostics();
		} else {
			message = problem.toString();
		}
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
