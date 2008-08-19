package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * The temporal logic R operator. x R y is true if y is true until the first
 * position in which x is true (or forever if such a position does not exist).
 * 
 * @author quark
 */
public class ReleasePredicate implements Predicate {
	private final Predicate predicate;
	public ReleasePredicate(final Predicate left, final Predicate right) {
		// a R b = a ; ~a, b, X (a R b)
		this.predicate = OrPredicate.or(
				left,
				AndPredicate.and(
						new NotPredicate(left),
						right,
						new NextPredicate(this)));
	}
	public Result evaluate(Frame frame) {
		return predicate.evaluate(frame);
	}
}
