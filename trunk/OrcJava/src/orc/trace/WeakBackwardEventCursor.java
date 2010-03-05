//
// WeakBackwardEventCursor.java -- Java class WeakBackwardEventCursor
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

import java.lang.ref.WeakReference;

import orc.trace.events.Event;

/**
 * Wrap a cursor to provide weak back references. This means you can only
 * traverse back to the earliest cursor for which you hold a reference
 * externally.
 * 
 * @author quark
 */
public class WeakBackwardEventCursor implements EventCursor {
	private final EventCursor cursor;
	private final WeakReference<WeakBackwardEventCursor> back;

	public WeakBackwardEventCursor(final EventCursor cursor) {
		this(cursor, (WeakReference<WeakBackwardEventCursor>) null);
	}

	private WeakBackwardEventCursor(final EventCursor cursor, final WeakBackwardEventCursor back) {
		this(cursor, new WeakReference<WeakBackwardEventCursor>(back));
	}

	private WeakBackwardEventCursor(final EventCursor cursor, final WeakReference<WeakBackwardEventCursor> back) {
		this.cursor = cursor;
		this.back = back;
		cursor.current().setCursor(this);
	}

	public Event current() {
		return cursor.current();
	}

	public WeakBackwardEventCursor forward() throws EndOfStream {
		return new WeakBackwardEventCursor(cursor.forward(), this);
	}

	public WeakBackwardEventCursor backward() throws EndOfStream {
		if (back == null) {
			throw new EndOfStream();
		}
		final WeakBackwardEventCursor out = back.get();
		if (out == null) {
			throw new EndOfStream();
		}
		return out;
	}
}
