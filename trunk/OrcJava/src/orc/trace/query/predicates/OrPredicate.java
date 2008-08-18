/**
 * 
 */
package orc.trace.query.predicates;

import orc.trace.query.Frame;

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
}