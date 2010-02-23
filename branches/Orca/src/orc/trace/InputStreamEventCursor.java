//
// InputStreamEventCursor.java -- Java class InputStreamEventCursor
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleInputStream;

public class InputStreamEventCursor implements EventCursor {
	private final HandleInputStream in;
	private final Event head;
	private InputStreamEventCursor tail;
	/**
	 * Sequence counter for events.
	 * FIXME: this won't reset if you open multiple event streams.
	 */
	private static long seq = 0;

	public InputStreamEventCursor(final InputStream in) throws IOException {
		this(new HandleInputStream(new GZIPInputStream(in)));
	}

	private InputStreamEventCursor(final HandleInputStream in) throws IOException {
		final Handle<Event> handle = in.readHandle();
		this.in = in;
		this.head = handle.get();
		this.head.setCursor(this);
		this.head.setSeq(seq++);
	}

	public Event current() {
		return head;
	}

	public InputStreamEventCursor forward() throws EndOfStream {
		if (tail != null) {
			return tail;
		}
		try {
			tail = new InputStreamEventCursor(in);
			return tail;
		} catch (final EOFException e) {
			throw new EndOfStream();
		} catch (final IOException e) {
			// FIXME: is there a better way to handle this?
			throw new OrcError(e);
		}
	}

	public InputStreamEventCursor backward() throws EndOfStream {
		throw new EndOfStream();
	}
}
