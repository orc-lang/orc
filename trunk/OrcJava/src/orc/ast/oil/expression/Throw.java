//
// Throw.java -- Java class Throw
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

import java.util.Set;

import orc.ast.oil.TokenContinuation;
import orc.ast.oil.visitor.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;

public class Throw extends Expression {

	public Expression exception;

	public Throw(final Expression e) {
		exception = e;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return exception == null ? 0 : exception.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Throw other = (Throw) obj;
		if (exception == null) {
			if (other.exception != null) {
				return false;
			}
		} else if (!exception.equals(other.exception)) {
			return false;
		}
		return true;
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		exception.addIndices(indices, depth);
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		/* TODO: thrown type = join of thrown type and this synthesized type */
		exception.typesynth(ctx);

		// throw e : Bot, so long as e is typable.
		return Type.BOT;
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Throw(exception.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		final TokenContinuation K = new TokenContinuation() {

			public void execute(final Token t) {
				final Object o = t.getResult();

				try {
					t.throwException(o);
				} catch (final TokenException e) {
					t.error(e);
				}
			}
		};
		exception.setPublishContinuation(K);
		exception.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		exception.enter(t.move(exception));
	}
}
