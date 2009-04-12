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
	private EventCursor cursor;
	private WeakReference<WeakBackwardEventCursor> back;
	
	public WeakBackwardEventCursor(EventCursor cursor) {
		this(cursor, (WeakReference<WeakBackwardEventCursor>)null);
	}
	
	private WeakBackwardEventCursor(EventCursor cursor, WeakBackwardEventCursor back) {
		this(cursor, new WeakReference<WeakBackwardEventCursor>(back));
	}
	
	private WeakBackwardEventCursor(EventCursor cursor, WeakReference<WeakBackwardEventCursor> back) {
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
		if (back == null) throw new EndOfStream();
		WeakBackwardEventCursor out = back.get();
		if (out == null) throw new EndOfStream();
		return out;
	}
}
