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

package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.Located;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * Annotate an expression with a source location.
 * @author quark
 */
public class WithLocation extends Expression implements Located {
	private final Expression expr;
	private final SourceLocation location;

	public WithLocation(final Expression expr, final SourceLocation location) {
		assert location != null;
		this.expr = expr;
		this.location = location;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.WithLocation(expr.convert(vars, typevars), location);
	}

	@Override
	public Expression subst(final Argument a, final FreeVariable x) {
		return new WithLocation(expr.subst(a, x), location);
	}

	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(final Type T, final FreeTypeVariable X) {
		return new WithLocation(expr.subst(T, X), location);
	}

	@Override
	public Set<Variable> vars() {
		return expr.vars();
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

	@Override
	public String toString() {
		return expr.toString(); // "{-" + location + "-}\n(" + expr +")";
	}
}
