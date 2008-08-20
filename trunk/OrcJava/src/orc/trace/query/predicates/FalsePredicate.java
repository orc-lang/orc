package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * Always false.
 */
public final class FalsePredicate implements Predicate {
	public static final FalsePredicate singleton = new FalsePredicate();
	private FalsePredicate() {}
	public Result evaluate(Frame _) {
		return null;
	}
}