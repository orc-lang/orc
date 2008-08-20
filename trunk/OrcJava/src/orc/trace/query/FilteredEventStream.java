package orc.trace.query;

import orc.trace.events.Event;
import orc.trace.query.EventStream.EndOfStream;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.Result;

public class FilteredEventStream implements EventStream {
	private EventStream stream;
	private final Predicate predicate;
	private Frame frame;
	
	public FilteredEventStream(EventStream stream, Predicate predicate) {
		this.stream = stream;
		this.predicate = predicate;
	}

	public Event head() throws EndOfStream {
		// repeat until we run out of elements or the
		// predicate evaluates to true
		while (true) {
			Result r1 = predicate.evaluate(Frame.newFrame(stream));
			if (r1 != null) {
				frame = r1.getFrame();
				return stream.head();
			}
			stream = stream.tail();
		}
	}
	
	public Frame frame() throws EndOfStream {
		head();
		return frame;
	}

	public FilteredEventStream tail() throws EndOfStream {
		return new FilteredEventStream(stream.tail(), predicate);
	}
}
