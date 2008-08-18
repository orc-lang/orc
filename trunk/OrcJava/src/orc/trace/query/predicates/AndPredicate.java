package orc.trace.query.predicates;

import orc.trace.query.Frame;

public class AndPredicate implements Predicate {
	private final Predicate left;
	private final Predicate right;
	public AndPredicate(Predicate left, Predicate right) {
		this.left = left;
		this.right = right;
	}
	public Result evaluate(Frame frame) {
		return new AndContinuation(
				new PredicateContinuation(frame, left),
				right)
			.evaluate();
	}
	
	/**
	 * Utility method to AND an array of predicates.
	 * Right-associative for efficiency.
	 */
	public static Predicate and(Predicate ... ps) {
		if (ps.length == 0) return FalsePredicate.singleton;
		if (ps.length == 1) return ps[0];
		Predicate out = new AndPredicate(ps[ps.length-2], ps[ps.length-1]);
		for (int i = ps.length-3; i >= 0; --i) {
			out = new AndPredicate(ps[i], out);
		}
		return out;
	}
}