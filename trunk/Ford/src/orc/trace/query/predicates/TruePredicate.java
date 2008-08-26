package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * Always true.
 */
public final class TruePredicate implements Predicate {
	public static final TruePredicate singleton = new TruePredicate();
	private TruePredicate() {}
	public Result evaluate(Frame frame) {
		return new Result(frame);
	}
}