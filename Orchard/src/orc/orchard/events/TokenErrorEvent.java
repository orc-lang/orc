//
// TokenErrorEvent.java -- Java class TokenErrorEvent
// Project Orchard
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.Position;

import orc.compile.parse.PositionWithFilename;
import orc.error.OrcException;
import orc.error.runtime.JavaException;

public class TokenErrorEvent extends JobEvent {
    public String message;
    public String positionString;
    public String posFilename;
    public int posLine;
    public int posColumn;

    public TokenErrorEvent() {
    }

    public TokenErrorEvent(final Throwable problem) {
        if (problem instanceof OrcException) {
            final OrcException oe = (OrcException) problem;

            final Position pos = oe.getPosition();
            if (pos != null && !(pos instanceof NoPosition$)) {
                positionString = pos.toString();
                if (pos instanceof PositionWithFilename) {
                    posFilename = ((PositionWithFilename) pos).filename();
                }
                posLine = pos.line();
                posColumn = pos.column();
            }

            if (oe instanceof JavaException) {
                // Don't reveal Java stack trace
                final JavaException je = (JavaException) oe;
                message = je.getMessageAndPositon() + "\n" + je.getOrcStacktraceAsString();
            } else {
                message = oe.getMessageAndDiagnostics();
            }
        } else {
            message = problem.toString();
        }
    }

    @Override
    public <E> E accept(final Visitor<E> visitor) {
        return visitor.visit(this);
    }
}
