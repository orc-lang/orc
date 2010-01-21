//
// Sequential.java -- Java class Sequential
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
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;

public class Sequential extends Expression {

	public Expression left;
	public Expression right;

	/* An optional variable name, used for documentation purposes.
	 * It has no operational purpose, since the expression is already
	 * in deBruijn index form. 
	 */
	public String name;

	public Sequential(final Expression left, final Expression right, final String name) {
		this.left = left;
		this.right = right;
		this.name = name;
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth + 1); // Push binds a variable on the right
	}

	@Override
	public String toString() {
		return "(" + left.toString() + " >> " + right.toString() + ")";
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
		final Type ltype = left.typesynth(ctx);
		return right.typesynth(ctx.bindVar(ltype));
	}

	@Override
	public void typecheck(final TypingContext ctx, final Type T) throws TypeException {
		final Type ltype = left.typesynth(ctx);
		right.typecheck(ctx.bindVar(ltype), T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Sequential(left.marshal(), right.marshal(), name);
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		final TokenContinuation leftK = new TokenContinuation() {
			public void execute(final Token t) {
				final Object val = t.getResult();
				t.bind(val);
				t.move(right).activate();
			}
		};
		left.setPublishContinuation(leftK);
		final TokenContinuation rightK = new TokenContinuation() {
			public void execute(final Token t) {
				t.unwind();
				leave(t);
			}
		};
		right.setPublishContinuation(rightK);
		left.populateContinuations();
		right.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		t.move(left);
		left.enter(t);
	}
}
