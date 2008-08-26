package orc.trace.query.predicates;

import orc.trace.Term;
import orc.trace.query.Frame;

/**
 * Unify two terms.
 * @author quark
 */
public class EqualPredicate implements Predicate {
	private final Term left;
	private final Term right;
	public EqualPredicate(Term left, Term right) {
		this.left = left;
		this.right = right;
	}
	public Result evaluate(Frame frame) {
		Frame frame1 = frame.unify(left, right);
		if (frame1 == null) return null;
		else return new Result(frame1);
	}
}