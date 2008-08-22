package orc.trace.query;

import orc.trace.events.Event;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;

public class FilteredEventCursor implements EventCursor {
	private EventCursor cursor;
	private final Predicate predicate;
	private Frame frame;
	private boolean forward = true;
	
	public FilteredEventCursor(EventCursor cursor, Predicate predicate) {
		this(cursor, predicate, true);
	}
	
	public FilteredEventCursor(EventCursor cursor, Predicate predicate, boolean forward) {
		this.cursor = cursor;
		this.predicate = predicate;
		this.forward = forward;
	}

	public Event current() throws EndOfStream {
		// repeat until we run out of elements or the
		// predicate evaluates to true
		while (true) {
			Result r1 = predicate.evaluate(Frame.newFrame(cursor));
			if (r1 != null) {
				frame = r1.getFrame();
				return cursor.current();
			}
			if (forward) cursor = cursor.forward();
			else cursor = cursor.backward();
		}
	}
	
	public Frame frame() throws EndOfStream {
		current();
		return frame;
	}

	public FilteredEventCursor forward() throws EndOfStream {
		return new FilteredEventCursor(cursor.forward(), predicate, true);
	}

	public FilteredEventCursor backward() throws EndOfStream {
		return new FilteredEventCursor(cursor.backward(), predicate, false);
	}
}
