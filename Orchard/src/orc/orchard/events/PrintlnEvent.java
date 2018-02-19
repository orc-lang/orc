//
// PrintlnEvent.java -- Java class PrintlnEvent
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

/**
 * Whenever a "print" or "println" site is called, the output is buffered and
 * sent to the client with println events. Well-written Orc programs should use
 * publications to communicate with the client, but using prints is convenient
 * for short scripts.
 *
 * @author quark
 */
public class PrintlnEvent extends JobEvent {
    private static final long serialVersionUID = -6139293717658978262L;
    /**
     * The newline terminator is implicit, so that the client can use whatever
     * newlines are appropriate for their environment.
     */
    public String line;

    public PrintlnEvent() {
    }

    public PrintlnEvent(final String line) {
        this.line = line;
    }

    @Override
    public <E> E accept(final Visitor<E> visitor) {
        return visitor.visit(this);
    }
}
