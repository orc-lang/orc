package orc.trace.query.predicates;

import orc.trace.EventCursor.EndOfStream;
import orc.trace.query.Frame;

/**
 * Succeed if we are at the end of the stream.
 * 
 * @see NextPredicate
 */
public class EndPredicate implements Predicate {
	private final boolean forward;
	public EndPredicate(boolean forward) {
		this.forward = forward;
	}
	public Result evaluate(Frame frame) {
		try {
			if (forward) frame.forward();
			else frame.backward();
			return null;
		} catch (EndOfStream _) {
			return new Result(frame);
		}
	}
}
