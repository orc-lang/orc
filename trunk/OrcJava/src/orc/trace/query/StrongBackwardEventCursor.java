package orc.trace.query;

import java.lang.ref.WeakReference;

import orc.trace.events.Event;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;

/**
 * Wrap a cursor to provide strong backward references. This is likely to waste
 * memory but is the only way to allow arbitrary bidirectional queries.
 * 
 * @author quark
 */
public class StrongBackwardEventCursor implements EventCursor {
	private EventCursor cursor;
	private StrongBackwardEventCursor back;
	
	private StrongBackwardEventCursor(EventCursor cursor, StrongBackwardEventCursor back) {
		this.cursor = cursor;
		this.back = back;
	}
	
	public StrongBackwardEventCursor(EventCursor cursor) {
		this(cursor, null);
	}

	public Event current() throws EndOfStream {
		return cursor.current();
	}

	public StrongBackwardEventCursor forward() throws EndOfStream {
		return new StrongBackwardEventCursor(cursor.forward(), this);
	}

	public StrongBackwardEventCursor backward() throws EndOfStream {
		if (back == null) throw new EndOfStream();
		return back;
	}
}
