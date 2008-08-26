package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * Base class for predicates which just construct (and delegate to) a new
 * predicate based on their arguments.
 * 
 * @author quark
 */
public abstract class DerivedPredicate implements Predicate {
	private Predicate predicate;
	/**
	 * Use this to set the predicate instead of the constructor
	 * so that you're allowed to use this in the argument.
	 */
	protected void setPredicate(Predicate predicate) {
		this.predicate = predicate;
	}
	public Result evaluate(Frame frame) {
		return predicate.evaluate(frame);
	}
}
