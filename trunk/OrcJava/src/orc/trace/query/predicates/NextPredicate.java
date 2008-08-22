package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.EventCursor.EndOfStream;

/**
 * The temporal logic X (neXt) operator. Evaluate a predicate in the context of
 * the next (or previous) event in time. All of the temporal operators are based
 * on this, and they all take a flag to indicate whether they are operating
 * forward (or back) in time.
 */
public class NextPredicate implements Predicate {
	private final Predicate predicate;
	private final boolean forward;
	public NextPredicate(final Predicate predicate, boolean forward) {
		this.predicate = predicate;
		this.forward = forward;
	}
	public Result evaluate(final Frame frame) {
		try {
			// After evaluating the predicate at one position
			// forward, rewind the event stream
			return new AndContinuation(
					new PredicateContinuation(
							forward ? frame.forward() : frame.backward(),
							predicate),
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
