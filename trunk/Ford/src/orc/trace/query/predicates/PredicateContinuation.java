/**
 * 
 */
package orc.trace.query.predicates;

import orc.trace.query.Frame;

public class PredicateContinuation implements Continuation {
	private final Frame frame;
	private final Predicate predicate;
	public PredicateContinuation(final Frame frame, final Predicate predicate) {
		this.frame = frame;
		this.predicate = predicate;
	}
	public Result evaluate() {
		return predicate.evaluate(frame);
	}
}