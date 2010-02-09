//
// DeclareType.java -- Java class DeclareType
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
import orc.runtime.Token;
import orc.type.TypingContext;

/**
 * Bind a type in the given scope.
 * 
 * @author dkitchin
 */
public class DeclareType extends Expression {

	public orc.ast.oil.type.Type type;
	public Expression body;

	public DeclareType(final orc.ast.oil.type.Type type, final Expression body) {
		this.type = type;
		this.body = body;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (body == null ? 0 : body.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
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
		final DeclareType other = (DeclareType) obj;
		if (body == null) {
			if (other.body != null) {
				return false;
			}
		} else if (!body.equals(other.body)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		body.addIndices(indices, depth);
	}

	@Override
	public orc.type.Type typesynth(final TypingContext ctx) throws TypeException {
		final orc.type.Type actualType = ctx.promote(type);
		final TypingContext newctx = ctx.bindType(actualType);
		return body.typesynth(newctx);
	}

	@Override
	public void typecheck(final TypingContext ctx, final orc.type.Type T) throws TypeException {
		final orc.type.Type actualType = ctx.promote(type);
		final TypingContext newctx = ctx.bindType(actualType);
		body.typecheck(newctx, T);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.DeclareType(type.marshal(), body.marshal());
	}

	@Override
	public String toString() {
		return "type = " + type.toString() + "\n" + body.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		body.setPublishContinuation(getPublishContinuation());
		// Trigger a NullPointerException if this node's publish continutation is executed,
		// rather than its child (which would skip up the AST above this node)
		setPublishContinuation(null);
		body.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		body.enter(t.move(body));
	}
}
