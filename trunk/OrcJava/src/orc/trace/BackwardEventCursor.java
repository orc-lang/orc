package orc.trace;

import orc.trace.events.Event;

/**
 * Wrap a cursor to provide strong backward references. This is likely to waste
 * memory but is the only way to allow arbitrary bidirectional queries.
 * 
 * @author quark
 */
public class BackwardEventCursor implements EventCursor {
	private EventCursor cursor;
	private BackwardEventCursor back;
	
	private BackwardEventCursor(EventCursor cursor, BackwardEventCursor back) {
		this.cursor = cursor;
		this.back = back;
		cursor.current().setCursor(this);
	}
	
	public BackwardEventCursor(EventCursor cursor) {
		this(cursor, null);
	}

	public Event current() {
		return cursor.current();
	}

	public BackwardEventCursor forward() throws EndOfStream {
		return new BackwardEventCursor(cursor.forward(), this);
	}

	public BackwardEventCursor backward() throws EndOfStream {
		if (back == null) throw new EndOfStream();
		return back;
	}
}
