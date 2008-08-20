package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.EventStream.EndOfStream;
import orc.trace.query.patterns.BindingPattern;

/**
 * Unify the current event with a variable.
 * Used to bind or match the current event.
 */
public class CurrentEventPredicate implements Predicate {
	private BindingPattern variable;
	public CurrentEventPredicate(BindingPattern variable) {
		this.variable = variable;
	}
	public Result evaluate(Frame frame) {
		try {
			frame =  variable.unify(frame, frame.currentEvent());
			if (frame == null) return Result.NO;
			else return new Result(frame);
		} catch (EndOfStream e) {
			return Result.NO;
		}
	}
}
