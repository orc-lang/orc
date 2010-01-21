//
// HasType.java -- Java class HasType
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
import orc.ast.oil.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.Token;
import orc.type.TypingContext;

/**
 * An expression with an ascribed type.
 * 
 * @author dkitchin
 */
public class HasType extends Expression {

	public Expression body;
	public orc.ast.oil.type.Type type;
	public boolean checkable; // set to false if this is a type assertion, not a type ascription

	public HasType(final Expression body, final orc.ast.oil.type.Type type, final boolean checkable) {
		this.body = body;
		this.type = type;
		this.checkable = checkable;
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public <E, C> E accept(final ContextualVisitor<E, C> cvisitor, final C initialContext) {
		return cvisitor.visit(this, initialContext);
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		body.addIndices(indices, depth);
	}

	@Override
	public orc.type.Type typesynth(final TypingContext ctx) throws TypeException {

		final orc.type.Type actualType = ctx.promote(type);

		/* If this ascription can be checked, check it */
		if (checkable) {
			body.typecheck(ctx, actualType);
		}
		/* If not, it is a type assertion, so we do not check it. */
		else {
		}

		return actualType;
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.HasType(body.marshal(), type.marshal(), checkable);
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		body.setPublishContinuation(getPublishContinuation());
		body.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		body.enter(t);
	}
}
