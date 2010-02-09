//
// Expression.java -- Java class Expression
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.expression;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.TokenContinuation;
import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.visitor.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.Token;
import orc.type.TypingContext;

/**
 * Base class for the portable (.oil, for Orc Intermediate Language) abstract syntax tree.
 * 
 * @author dkitchin, jthywiss
 */
public abstract class Expression {
	transient private TokenContinuation publishContinuation;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	abstract public int hashCode();

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	abstract public boolean equals(Object obj);

	/* Typechecking */

	/* Given a context, infer this expression's type */
	public abstract orc.type.Type typesynth(TypingContext ctx) throws TypeException;

	/* Check that this expression has type T in the given context. 
	 * 
	 * Some expressions will always have inferred types, so
	 * the default checking behavior is to infer the type and make
	 * sure that the inferred type is a subtype of the checked type.
	 */
	public void typecheck(final TypingContext ctx, final orc.type.Type T) throws TypeException {
		final orc.type.Type S = typesynth(ctx);
		if (!S.subtype(T)) {
			throw new SubtypeFailureException(S, T);
		}
	}

	/**
	 * Find the set of free variables in this expression. 
	 * 
	 * @return 	The set of free variables.
	 */
	public final Set<Variable> freeVars() {
		final Set<Integer> indices = new TreeSet<Integer>();
		this.addIndices(indices, 0);

		final Set<Variable> vars = new TreeSet<Variable>();
		for (final Integer i : indices) {
			vars.add(new Variable(i));
		}

		return vars;
	}

	/**
	 * If this expression has any indices which are >= depth,
	 * add (index - depth) to the index set accumulator. The depth 
	 * increases each time this method recurses through a binder.
	 * 
	 * The default implementation is to assume the expression
	 * has no free variables, and thus do nothing. Expressions
	 * which contain variables or subexpressions override this
	 * behavior.
	 * 
	 * @param indices   The index set accumulator.
	 * @param depth    The minimum index for a free variable.
	 */
	public abstract void addIndices(Set<Integer> indices, int depth);

	public abstract <E> E accept(Visitor<E> visitor);

	//public abstract <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext);

	public abstract orc.ast.xml.expression.Expression marshal() throws CompilationException;

	/**
	 * Convenience method, to marshal a list of expressions.
	 */
	public static orc.ast.xml.expression.Expression[] marshalAll(final List<Expression> es) throws CompilationException {

		final orc.ast.xml.expression.Expression[] newes = new orc.ast.xml.expression.Expression[es.size()];
		int i = 0;
		for (final Expression e : es) {
			newes[i++] = e.marshal();
		}

		return newes;
	}

	public abstract void populateContinuations();

	/**
	 * @return the publishContinuation
	 */
	public TokenContinuation getPublishContinuation() {
		return publishContinuation;
	}

	/**
	 * @param publishContinuation the publishContinuation to set
	 */
	public void setPublishContinuation(final TokenContinuation publishContinuation) {
		this.publishContinuation = publishContinuation;
	}

	abstract public void enter(Token t);

	public void leave(final Token t) {
		getPublishContinuation().execute(t);
	}
}
