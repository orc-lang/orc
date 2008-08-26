package orc.trace.query.patterns;

import orc.trace.Term;
import orc.trace.query.Frame;

/**
 * A {@link Variable} or {@link PropertyPattern} which may be bound.
 * @see Frame#unify(Term, Term)
 * @author quark
 */
public abstract class BindingPattern extends Pattern {}
