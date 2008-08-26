package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * Alternative.
 */
public class OrPredicate implements Predicate {
	private final Predicate left;
	private final Predicate right;
	public OrPredicate(Predicate left, Predicate right) {
		this.left = left;
		this.right = right;
	}
	public Result evaluate(Frame frame) {
		return new OrContinuation(
				new PredicateContinuation(frame, left),
				new PredicateContinuation(frame, right))
			.evaluate();
	}
	/**
	 * Utility method to OR an array of predicates.
	 * Right-associative to optimize backtracking.
	 */
	public static Predicate or(Predicate ... ps) {
		if (ps.length == 0) return TruePredicate.singleton;
		if (ps.length == 1) return ps[0];
		Predicate out = new OrPredicate(ps[ps.length-2], ps[ps.length-1]);
		for (int i = ps.length-3; i >= 0; --i) {
			out = new OrPredicate(ps[i], out);
		}
		return out;
	}
}