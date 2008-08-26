package orc.trace.query.predicates;


/**
 * The temporal logic W (Weak until) operator. x W y = x U y or G x.
 * 
 * @see UntilPredicate
 * @see NextPredicate
 * @author quark
 */
public class WeakUntilPredicate extends DerivedPredicate {
	public WeakUntilPredicate(final Predicate left, final Predicate right, final boolean forward) {
		// a W b = b ; a, ( (X a W b) ; (~ X true) )
		setPredicate(new OrPredicate(
				right,
				new AndPredicate(
						left,
						new OrPredicate(
							new EndPredicate(forward),
							new NextPredicate(this, forward)))));
	}
}
