package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.EventStream.EndOfStream;

/**
 * The temporal logic X operator. Evaluate a predicate
 * in the context of the next event in time.
 */
public class NextPredicate implements Predicate {
	private final Predicate predicate;
	public NextPredicate(final Predicate predicate) {
		this.predicate = predicate;
	}
	public Result evaluate(final Frame frame) {
		try {
			// After evaluating the predicate at one position
			// forward, rewind the event stream
			return new AndContinuation(
					new PredicateContinuation(frame.forward(), predicate),
					new Predicate() {
						public Result evaluate(Frame nframe) {
							return new Result(nframe.rewind(frame));
						}
					}).evaluate();
		} catch (EndOfStream _) {
			return null;
		}
	}
}
