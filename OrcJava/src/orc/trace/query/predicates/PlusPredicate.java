package orc.trace.query.predicates;

import orc.trace.query.Frame;

public class PlusPredicate implements Predicate {
	private final Predicate predicate;
	public PlusPredicate(final Predicate predicate) {
		this.predicate = predicate;
	}
	public Result evaluate(Frame frame) {
		Result r1 = predicate.evaluate(frame);
		if (r1 == null) return null;
		return new Result(r1.frame, new OrContinuation(
				r1.failure,
				new AndContinuation(
						new PredicateContinuation(
								r1.frame,
								NextEventPredicate.singleton),
						this)));
	}
}
