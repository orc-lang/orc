package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.patterns.BindingPattern;

public interface Predicate {
	/**
	 * Evaluate the predicate and update the frame with the resulting bindings.
	 * If the predicate cannot be satisfied, return null. Otherwise return the
	 * next alternative.
	 * 
	 * @param frame
	 * @return
	 */
	public Result evaluate(Frame frame);
}
