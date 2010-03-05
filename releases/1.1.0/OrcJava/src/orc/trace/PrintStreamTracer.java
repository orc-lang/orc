//
// PrintStreamTracer.java -- Java class PrintStreamTracer
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

package orc.trace;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;

/**
 * Write trace events to stdout in human-readable form.
 * FIXME: make which events are written configurable.
 * 
 * @author quark
 */
public final class PrintStreamTracer extends AbstractTracer {
	private final OutputStreamWriter out;
	private int seq = 0;

	public PrintStreamTracer(final OutputStream out) {
		this.out = new OutputStreamWriter(out);
	}

	@Override
	protected synchronized void record(final Handle<? extends Event> event) {
		try {
			event.get().setSeq(seq++);
			event.get().prettyPrint(out, 0);
			out.write('\n');
			out.flush();
		} catch (final IOException e) {
			// FIXME: is there a better way to handle this?
			// I don't want to pass the exception on to the
			// caller since there's no way to recover from
			// it.  Maybe we should just print an error
			// message rather than kill the whole engine.
			throw new OrcError(e);
		}
	}

	@Override
	public void finish() {
	}
}
