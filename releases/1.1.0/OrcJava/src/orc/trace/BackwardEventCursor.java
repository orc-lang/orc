//
// BackwardEventCursor.java -- Java class BackwardEventCursor
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

import orc.trace.events.Event;

/**
 * Wrap a cursor to provide strong backward references. This is likely to waste
 * memory but is the only way to allow arbitrary bidirectional queries.
 * 
 * @author quark
 */
public class BackwardEventCursor implements EventCursor {
	private final EventCursor cursor;
	private final BackwardEventCursor back;

	private BackwardEventCursor(final EventCursor cursor, final BackwardEventCursor back) {
		this.cursor = cursor;
		this.back = back;
		cursor.current().setCursor(this);
	}

	public BackwardEventCursor(final EventCursor cursor) {
		this(cursor, null);
	}

	public Event current() {
		return cursor.current();
	}

	public BackwardEventCursor forward() throws EndOfStream {
		return new BackwardEventCursor(cursor.forward(), this);
	}

	public BackwardEventCursor backward() throws EndOfStream {
		if (back == null) {
			throw new EndOfStream();
		}
		return back;
	}
}
