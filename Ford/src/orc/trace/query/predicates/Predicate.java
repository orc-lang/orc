package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.patterns.BindingPattern;

public interface Predicate {
	/**
	 * Evaluate the predicate using the given frame of variable bindings.
	 * Return a result containing the frame extended with any new variable
	 * bindings made while evaluating the predicate, plus a continuation
	 * for the next alternative result produced by the predicate.
	 * 
	 * @param frame
	 * @return null if the predicate cannot be satisfied
	 */
	public Result evaluate(Frame frame);
}
