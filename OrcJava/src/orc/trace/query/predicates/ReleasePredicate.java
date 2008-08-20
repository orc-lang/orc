package orc.trace.query.predicates;

import orc.trace.query.Frame;

/**
 * The temporal logic R operator. x R y is true if y is true through the first
 * position in which x is true (or forever if such a position does not exist).
 * 
 * <p>The key difference between R and U is that R requires that both x and y
 * are true at some point if y ever becomes false.
 * 
 * @see UntilPredicate
 * @author quark
 */
public class ReleasePredicate extends DerivedPredicate {
	public ReleasePredicate(final Predicate left, final Predicate right) {
		// a R b = b , (a ; (~ X true) ; (X a R b))
		setPredicate(new AndPredicate(
				right,
				OrPredicate.or(
						new EndPredicate(),
						left,
						new NextPredicate(this))));
	}
}
