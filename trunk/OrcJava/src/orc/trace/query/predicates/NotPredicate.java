package orc.trace.query.predicates;

import orc.trace.query.Frame;

public class NotPredicate implements Predicate {
	private final Predicate predicate;
	public NotPredicate(Predicate predicate) {
		this.predicate = predicate;
	}
	public Result evaluate(Frame frame) {
		Result r1 = predicate.evaluate(frame);
		if (r1 == Result.NO) return new Result(frame);
		else return null;
	}
}