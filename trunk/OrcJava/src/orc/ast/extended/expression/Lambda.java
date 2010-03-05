//
// Lambda.java -- Java class Lambda
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.expression;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.declaration.def.AggregateDef;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.error.compiletime.CompilationException;

public class Lambda extends Expression {

	public List<List<Pattern>> formals;
	public Expression body;
	public Type resultType; /* optional, may be null */
	public List<String> typeFormals;

	public Lambda(final List<List<Pattern>> formals, final Expression body, final Type resultType, final List<String> typeFormals) {
		this.formals = formals;
		this.body = body;
		this.resultType = resultType;
		this.typeFormals = typeFormals;
	}

	public static Lambda makeThunk(final Expression body) {
		LinkedList<List<Pattern>> outerFormals = new LinkedList<List<Pattern>>();
		outerFormals.add(new LinkedList<Pattern>());
		return new Lambda(outerFormals, body, null, new LinkedList<String>());
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		// Create a new aggregate definition
		final AggregateDef ad = new AggregateDef();

		// Populate the aggregate with a single clause for this anonymous function
		final DefMemberClause singleton = new DefMemberClause("", formals, body, resultType, typeFormals, false);
		singleton.setSourceLocation(getSourceLocation());
		singleton.extend(ad);

		// Make a simple AST definition group with one definition created from the aggregate
		final List<orc.ast.simple.expression.Def> defs = new LinkedList<orc.ast.simple.expression.Def>();
		defs.add(ad.simplify());

		// Bind the definition in a scope which simply publishes it
		final Variable f = ad.getVar();
		return new orc.ast.simple.expression.DeclareDefs(defs, new orc.ast.simple.expression.Let(f));
	}

	@Override
	public String toString() {
		return "(lambda (" + join(formals, ", ") + ") = " + body + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
