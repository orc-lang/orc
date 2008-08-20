package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * The temporal logic U operator. x U y means that either y holds now, or y
 * holds at some point in the future and x holds from now until that point.
 * 
 * @author quark
 */
public class UntilPredicate implements Predicate {
	private final Predicate predicate;
	public UntilPredicate(final Predicate left, final Predicate right) {
		// a U b = b ; a, X a U b
		this.predicate = new OrPredicate(
				right,
				new AndPredicate(
						left,
						new NextPredicate(this)));
	}
	public Result evaluate(Frame frame) {
		return predicate.evaluate(frame);
	}
}
