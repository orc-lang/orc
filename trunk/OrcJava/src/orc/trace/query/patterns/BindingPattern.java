package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.query.Frame;
import orc.trace.query.Term;

/**
 * A {@link Variable} or {@link PropertyPattern} which may be bound.
 * @see Frame#unify(Term, Term)
 * @author quark
 */
public abstract class BindingPattern extends Pattern {}
