//
// WithLocation.java -- Java class WithLocation
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
import orc.error.Located;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * Annotate an expression with a source location.
 * @author quark
 */
public class WithLocation extends Expression implements Located {
	public final Expression body;
	public final SourceLocation location;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (body == null ? 0 : body.hashCode());
		result = prime * result + (location == null ? 0 : location.hashCode());
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
		final WithLocation other = (WithLocation) obj;
		if (body == null) {
			if (other.body != null) {
				return false;
			}
		} else if (!body.equals(other.body)) {
			return false;
		}
		if (location == null) {
			if (other.location != null) {
				return false;
			}
		} else if (!location.equals(other.location)) {
			return false;
		}
		return true;
	}

	public WithLocation(final Expression expr, final SourceLocation location) {
		this.body = expr;
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		try {
			return body.typesynth(ctx);
		} catch (final TypeException e) {
			/* If this error has no location, give it this (least enclosing) location */
			if (e.getSourceLocation() == null || e.getSourceLocation().isUnknown()) {
				e.setSourceLocation(location);
			}
			throw e;
		}
	}

	@Override
	public void typecheck(final TypingContext ctx, final Type T) throws TypeException {
		try {
			body.typecheck(ctx, T);
		} catch (final TypeException e) {
			/* If this error has no location, give it this (least enclosing) location */
			if (e.getSourceLocation() == null || e.getSourceLocation().isUnknown()) {
				e.setSourceLocation(location);
			}
			throw e;
		}
	}

	@Override
	public String toString() {
		//return "{-" + location + "-}\n(" + body + ")";
		return body.toString();
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		body.addIndices(indices, depth);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.WithLocation(body.marshal(), location);
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
		t.setSourceLocation(location);
		body.enter(t.move(body));
	}
}
