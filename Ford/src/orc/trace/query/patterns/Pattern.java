package orc.trace.query.patterns;

import orc.trace.Term;
import orc.trace.Terms;
import orc.trace.query.Frame;

/**
 * This interface exists to distinguish terms
 * which may contain variables.
 * @author quark
 */
public abstract class Pattern implements Term {
	public String toString() {
		return Terms.printToString(this);
	}
	/**
	 * Unify this with that using the given environment.
	 * Return null if the terms cannot be unified.
	 * Don't call this directly, use {@link Frame#unify(Term, Term)}.
	 * If this is not an instance of {@link Variable}, then neither is that.
	 * If this is not an instance of {@link BindingPattern}, then neither is that.
	 */
	public abstract Frame unify(Frame frame, Term that);
	/**
	 * Substitute bound variables. If there are any unbound variables,
	 * this may return a pattern.
	 */
	public abstract Term evaluate(Frame frame);
	/**
	 * Check if a variable occurs in this term.
	 */
	public abstract boolean occurs(Variable var);
}
