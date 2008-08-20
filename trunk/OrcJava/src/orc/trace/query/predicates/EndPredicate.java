package orc.trace.query.predicates;

import java.util.NoSuchElementException;

import orc.trace.query.Frame;

/**
 * Succeed if we are at the end of the stream.
 * Shortcut for ~ X true.
 */
public class EndPredicate implements Predicate {
	public Result evaluate(Frame frame) {
		try {
			frame.forward();
			return null;
		} catch (NoSuchElementException _) {
			return new Result(frame);
		}
	}
}
