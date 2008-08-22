package orc.trace.query;

import java.lang.ref.WeakReference;

import orc.trace.events.Event;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;

/**
 * Wrap a cursor to provide weak back references. This means you can only
 * traverse back to the earliest cursor for which you hold a reference
 * externally.
 * 
 * @author quark
 */
public class WeakBackwardEventCursor implements EventCursor {
	private EventCursor cursor;
	private WeakReference<WeakBackwardEventCursor> back;
	
	private WeakBackwardEventCursor(EventCursor cursor, WeakBackwardEventCursor back) {
		this.cursor = cursor;
		this.back = new WeakReference<WeakBackwardEventCursor>(back);
	}
	
	public WeakBackwardEventCursor(EventCursor cursor) {
		this.cursor = cursor;
		this.back = null;
	}

	public Event current() throws EndOfStream {
		return cursor.current();
	}

	public WeakBackwardEventCursor forward() throws EndOfStream {
		return new WeakBackwardEventCursor(cursor.forward(), this);
	}

	public WeakBackwardEventCursor backward() throws EndOfStream {
		if (back == null) throw new EndOfStream();
		WeakBackwardEventCursor out = back.get();
		if (out == null) throw new EndOfStream();
		return out;
	}
}
