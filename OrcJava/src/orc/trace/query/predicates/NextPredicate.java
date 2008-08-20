package orc.trace.query.predicates;

import java.util.NoSuchElementException;

import orc.trace.query.Frame;

/**
 * The temporal logic X operator. Evaluate a predicate
 * in the context of the next event in time.
 */
public class NextPredicate implements Predicate {
	private final Predicate predicate;
	public NextPredicate(final Predicate predicate) {
		this.predicate = predicate;
	}
	public Result evaluate(Frame frame) {
		try {
			return predicate.evaluate(frame.nextEvent());
		} catch (NoSuchElementException e) {
			return null;
		}
	}
}
