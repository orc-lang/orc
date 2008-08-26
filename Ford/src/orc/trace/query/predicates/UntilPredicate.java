package orc.trace.query.predicates;


/**
 * The temporal logic U (Until) operator. x U y means that either y holds now,
 * or y holds at some point in the future and x holds from now until that point.
 * 
 * @see NextPredicate
 * @author quark
 */
public class UntilPredicate extends DerivedPredicate {
	public UntilPredicate(final Predicate left, final Predicate right, boolean forward) {
		// a U b = b ; a, X a U b
		setPredicate(new OrPredicate(
				right,
				new AndPredicate(
						left,
						new NextPredicate(this, forward))));
	}
}
