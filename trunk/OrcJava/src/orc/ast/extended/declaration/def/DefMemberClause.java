//
// DefMemberClause.java -- Java class DefMemberClause
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

package orc.ast.extended.declaration.def;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.expression.Expression;
import orc.ast.extended.expression.HasType;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Definitions are scoped in the abstract syntax tree through a Declare containing
 * a DefsDeclaration. 
 * 
 * @author dkitchin
 *
 */

public class DefMemberClause extends DefMember {

	public List<List<Pattern>> formals;
	public List<String> typeFormals = null;
	public Expression body;
	public Type resultType; // May be null
	public boolean strict; // Some contexts (such as capsule) force clauses to be strict

	public DefMemberClause(final String name, final List<List<Pattern>> formals, final Expression body, final Type resultType, final List<String> typeFormals, final boolean strict) {
		this.name = name; /* name is "" when used for anonymous functions */
		this.formals = formals;
		this.body = body;
		this.resultType = resultType;
		this.typeFormals = typeFormals;
		this.strict = strict;
	}

	@Override
	public String toString() {
		return (name.equals("") ? "lambda" : "def ") + sigToString() + " = " + body;
	}

	public String sigToString() {
		final StringBuilder s = new StringBuilder();

		s.append(name);
		for (final List<Pattern> ps : formals) {
			s.append('(');
			if (ps != null) {
			  s.append(Expression.join(ps, ","));
			}
			s.append(')');
		}

		return s.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public void extend(final AggregateDef adef) throws CompilationException {

		final List<Pattern> phead = formals.get(0);
		List<Pattern> newformals = new LinkedList<Pattern>();
		List<Type> argTypes = new LinkedList<Type>();

		for (final Pattern p : phead) {
			/* Strip a toplevel type ascription from every argument pattern */
			if (p instanceof TypedPattern) {
				final TypedPattern tp = (TypedPattern) p;
				argTypes.add(tp.t);
				newformals.add(tp.p);
			} else {
				newformals = phead;

				/* There is at least one argument with a missing annotation.
				 * Request inference.
				 */
				argTypes = null;

				break;
			}
		}
		if (argTypes != null) {
			adef.setArgTypes(argTypes);
		}

		Expression newbody = body;

		if (formals.size() > 1) {
			final List<List<Pattern>> ptail = formals.subList(1, formals.size());
			if (resultType != null) {
				newbody = new HasType(newbody, resultType);
			}
			newbody = Expression.uncurry(ptail, newbody);
		}

		if (resultType != null) {
			adef.setResultType(resultType);
		}

		if (typeFormals != null) {
			adef.setTypeParams(typeFormals);
		}
		
		adef.setStrictness(strict);

		adef.addClause(new Clause(newformals, newbody));

		adef.addLocation(getSourceLocation());

	}
}
