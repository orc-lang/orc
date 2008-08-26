package orc.trace.query;

import orc.trace.events.Event;
import orc.trace.query.EventCursor.EndOfStream;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;

public class FilteredEventCursor implements EventCursor {
	private final EventCursor cursor;
	private final Predicate predicate;
	private final Frame frame;
	
	/**
	 * If the current event does not match the predicate, moves forward to find
	 * one that does.
	 */
	public static FilteredEventCursor newForward(EventCursor cursor, Predicate predicate) throws EndOfStream {
		EventCursor next = cursor;
		Result result = predicate.evaluate(new Frame(next));
		while (result == null) {
			next = next.forward();
			result = predicate.evaluate(new Frame(next));
		}
		return new FilteredEventCursor(next, predicate, result.getFrame());
	}
	
	/**
	 * If the current event does not match the predicate, moves backward to find
	 * one that does.
	 */
	public static FilteredEventCursor newBackward(EventCursor cursor, Predicate predicate) throws EndOfStream {
		EventCursor next = cursor;
		Result result = predicate.evaluate(new Frame(next));
		while (result == null) {
			next = next.backward();
			result = predicate.evaluate(new Frame(next));
		}
		return new FilteredEventCursor(next, predicate, result.getFrame());
	}
	
	private FilteredEventCursor(EventCursor cursor, Predicate predicate, Frame frame) {
		this.cursor = cursor;
		this.predicate = predicate;
		this.frame = frame;
		cursor.current().setCursor(this);
	}

	public Event current() {
		return cursor.current();
	}
	
	public Frame getFrame() {
		return frame;
	}

	public FilteredEventCursor forward() throws EndOfStream {
		return FilteredEventCursor.newForward(cursor.forward(), predicate);
	}

	public FilteredEventCursor backward() throws EndOfStream {
		return FilteredEventCursor.newBackward(cursor.forward(), predicate);
	}
}
