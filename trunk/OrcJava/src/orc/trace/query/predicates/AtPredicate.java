package orc.trace.query.predicates;

import orc.trace.events.Event;
import orc.trace.query.Frame;
import orc.trace.query.Term;
import orc.trace.query.EventCursor.EndOfStream;
import orc.trace.query.patterns.BindingPattern;

/**
 * Evaluate a predicate in the context of an event.
 */
public class AtPredicate extends DerivedPredicate {
	private final BindingPattern v;
	private final Predicate predicate;
	public AtPredicate(final BindingPattern v, final Predicate predicate) {
		this.v = v;
		this.predicate = predicate;
	}
	public Result evaluate(final Frame frame) {
		Term term = v.evaluate(frame);
		if (term instanceof Event) {
			Event event = (Event) term;
			// After evaluating the predicate at the
			// event's cursor, rewind the event stream
			return new AndContinuation(
					new PredicateContinuation(
							frame.at(event.getCursor()),
							predicate),
					new Predicate() {
						public Result evaluate(Frame nframe) {
							return new Result(nframe.rewind(frame));
						}
					}).evaluate();
		} else {
			return null;
		}
	}
}
