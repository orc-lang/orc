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
}