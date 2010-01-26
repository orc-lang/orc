//
// Otherwise.java -- Java class Otherwise
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

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.TokenContinuation;
import orc.ast.oil.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.Token;
import orc.runtime.regions.SemiRegion;
import orc.type.Type;
import orc.type.TypingContext;

public class Otherwise extends Expression {

	public Expression left;
	public Expression right;

	public Otherwise(final Expression left, final Expression right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}

	@Override
	public String toString() {
		return "(" + left.toString() + " ; " + right.toString() + ")";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public <E, C> E accept(final ContextualVisitor<E, C> cvisitor, final C initialContext) {
		return cvisitor.visit(this, initialContext);
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
		return new orc.ast.xml.expression.Otherwise(left.marshal(), right.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		final TokenContinuation leftK = new TokenContinuation() {
			public void execute(final Token t) {
				// This cast cannot fail; a Leave node always matches a Semi node earlier in the dag.
				final SemiRegion region = (SemiRegion) t.getRegion();

				// If a publication successfully leaves a SemiRegion, the right hand side of the semicolon shouldn't execute.
				// This step cancels the RHS.
				// It is an idempotent operation.
				region.cancel();

				leave(t.setRegion(region.getParent()));
			}
		};
		left.setPublishContinuation(leftK);
		right.setPublishContinuation(getPublishContinuation());
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
		forked.setQuiescent();
		final SemiRegion region = new SemiRegion(t.getRegion(), forked.move(right));
		left.enter(t.move(left).setRegion(region));
	}
}
