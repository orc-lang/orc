package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.EventCursor.EndOfStream;

/**
 * Succeed if we are at the end of the stream.
 * Shortcut for ~ X true.
 */
public class EndPredicate implements Predicate {
	public Result evaluate(Frame frame) {
		try {
			frame.forward();
			return null;
		} catch (EndOfStream _) {
			return new Result(frame);
		}
	}
}
