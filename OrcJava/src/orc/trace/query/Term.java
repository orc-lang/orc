package orc.trace.query;

import java.io.IOException;
import java.io.Writer;

import orc.trace.events.Event;
import orc.trace.query.patterns.Pattern;
import orc.trace.query.patterns.Variable;
import orc.trace.values.Value;

/**
 * Terms include {@link Value}s, {@link Event}s, and {@link Pattern}s.
 * {@link Pattern}s are the only terms that can include variables. The
 * separation is necessary to enforce constraints on values (for example, values
 * can be serialized in a trace file but pattern variables cannot), and also
 * because it makes the binary-dispatch nature of unification more
 * straightforward to implement. However it does lead to some duplication of
 * boilerplate code and class hierarchies.
 * 
 * @author quark
 */
public interface Term {
	/**
	 * Unify this with that using the given environment.
	 * Don't call this directly, use {@link Frame#unify(Term, Term)}.
	 * If this is not an instance of {@link Variable}, then neither is that.
	 * If this is not an instance of {@link Pattern}, then neither is that.
	 */
	public boolean unify(Frame frame, Term that);
	/**
	 * Substitute bound variables.
	 */
	public Term substitute(Frame frame);
	/**
	 * Check if a variable occurs in this term.
	 */
	public boolean occurs(Variable var);
	/**
	 * Pretty-print the term to out. If the term is multiple lines, each newline
	 * should be followed by at least indent tabs. The value should not begin or
	 * end with a newline.
	 */
	public void prettyPrint(Writer out, int indent) throws IOException;
}
