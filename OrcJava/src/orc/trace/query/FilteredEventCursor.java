package orc.trace.query;

import orc.trace.events.Event;
import orc.trace.query.EventCursor.EndOfStream;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;

public class FilteredEventCursor implements EventCursor {
	private EventCursor stream;
	private final Predicate predicate;
	private Frame frame;
	private boolean forward = true;
	
	public FilteredEventCursor(EventCursor stream, Predicate predicate) {
		this(stream, predicate, true);
	}
	
	public FilteredEventCursor(EventCursor stream, Predicate predicate, boolean forward) {
		this.stream = stream;
		this.predicate = predicate;
		this.forward = forward;
	}

	public Event current() throws EndOfStream {
		// repeat until we run out of elements or the
		// predicate evaluates to true
		while (true) {
			Result r1 = predicate.evaluate(Frame.newFrame(stream));
			if (r1 != null) {
				frame = r1.getFrame();
				return stream.current();
			}
			if (forward) stream = stream.forward();
			else stream = stream.back();
		}
	}
	
	public Frame frame() throws EndOfStream {
		current();
		return frame;
	}

	public FilteredEventCursor forward() throws EndOfStream {
		return new FilteredEventCursor(stream.forward(), predicate, true);
	}

	public EventCursor back() throws EndOfStream {
		return new FilteredEventCursor(stream.back(), predicate, false);
	}
}
