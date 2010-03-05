//
// Parallel.java -- Java class Parallel
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

import orc.ast.oil.visitor.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;

public class Parallel extends Expression {

	public Expression left;
	public Expression right;

	public Parallel(final Expression left, final Expression right) {
		this.left = left;
		this.right = right;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (left == null ? 0 : left.hashCode());
		result = prime * result + (right == null ? 0 : right.hashCode());
		return result;
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
		final Parallel other = (Parallel) obj;
		if (left == null) {
			if (other.left != null) {
				return false;
			}
		} else if (!left.equals(other.left)) {
			return false;
		}
		if (right == null) {
			if (other.right != null) {
				return false;
			}
		} else if (!right.equals(other.right)) {
			return false;
		}
		return true;
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}

	@Override
	public String toString() {
		return "(" + left.toString() + " | " + right.toString() + ")";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {

		final Type L = left.typesynth(ctx);
		final Type R = right.typesynth(ctx);
		return L.join(R);
	}

	@Override
	public void typecheck(final TypingContext ctx, final Type T) throws TypeException {

		left.typecheck(ctx, T);
		right.typecheck(ctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Parallel(left.marshal(), right.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		left.setPublishContinuation(getPublishContinuation());
		right.setPublishContinuation(getPublishContinuation());
		// Trigger a NullPointerException if this node's publish continutation is executed,
		// rather than its child (which would skip up the AST above this node)
		setPublishContinuation(null);
		left.populateContinuations();
		right.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		Token forked;
		try {
			forked = t.fork();
		} catch (final TokenLimitReachedError e) {
			t.error(e);
			return;
		}
		t.move(left).activate(); // Let the engine schedule this token
		forked.move(right).activate(); // Let the engine schedule this token
	}
}
