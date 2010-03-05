//
// OutputStreamTracer.java -- Java class OutputStreamTracer
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
import java.util.zip.GZIPOutputStream;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleOutputStream;

/**
 * Serialize and gzip events to an {@link OutputStream}.
 * The output stream will be closed when the execution is finished.
 * You can read the events back with an {@link InputStreamEventCursor}.
 * 
 * @author quark
 */
public class OutputStreamTracer extends AbstractTracer {
	/**
	 * How often should the output stream reset to conserve
	 * memory?  We should pick a value empirically, erring
	 * on the side of caution.  I pulled 100 out of my ass.
	 */
	private static final int STREAM_RESET_INTERVAL = 100;
	/** Output stream for events. */
	private final HandleOutputStream out;

	public OutputStreamTracer(final OutputStream out) throws IOException {
		this.out = new HandleOutputStream(new GZIPOutputStream(out), STREAM_RESET_INTERVAL);
	}

	@Override
	protected synchronized void record(final Handle<? extends Event> event) {
		try {
			// Handles manage sharing explicitly, so no need
			// to use the implicit writeObject sharing management
			out.writeUnshared(event);
			out.maybeReset();
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
		try {
			out.close();
		} catch (final IOException e) {
			throw new OrcError(e);
		}
	}
}
