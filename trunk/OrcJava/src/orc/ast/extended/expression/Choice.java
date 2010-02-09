//
// Choice.java -- Java class Choice
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

import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.type.Constructor;
import orc.ast.extended.declaration.type.DatatypeDeclaration;
import orc.ast.extended.pattern.CallPattern;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.type.NamedType;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class Choice extends Expression {

	public List<Expression> choices;

	public Choice(final List<Expression> choices) {
		this.choices = choices;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		/*
		 * Break up the expressions into their constituents.
		 * Each expression must be of the form:
		 * 
		 * val p = F
		 * G
		 * 
		 * Then, reform those constituents into the components
		 * of the encoded form:
		 *
		 * type branch[a,b...] = A(a) | B(b) ...
		 * val temp = A(F) | B(F') ...
		 * temp >A(p)> G | temp >B(p')> G' ...
		 * 
		 * 
		 */

		// branch
		// (manufacture a unique variable name)
		final String branch = "_branch" + choices.hashCode();

		// temp
		// (manufacture a unique variable name)
		final String temp = "_temp" + choices.hashCode();

		// [a,b,...]
		final List<String> typevars = new LinkedList<String>();

		// A(a) | B(b) ...
		final List<Constructor> constructors = new LinkedList<Constructor>();

		// A(F) | B(F') ...
		Expression competitors = new Stop();

		// temp >A(p)> G | temp >B(p')> G' ...
		Expression consequents = new Stop();

		for (final Expression c : choices) {
			Declare decl;
			ValDeclaration vald;
			try {
				decl = (Declare) c;
				vald = (ValDeclaration) decl.d;
			} catch (final ClassCastException cce) {
				throw new CompilationException("Subexpressions of ++ must be a val declaration followed by an expression.");
			}

			final Pattern p = vald.p;
			final Expression F = vald.e;
			final Expression G = decl.e;

			// Manufacture unique variable names
			final String tag = "_tag" + c.hashCode();
			final String typevar = "_tv" + c.hashCode();

			typevars.add(typevar);

			final List<Type> ts = new LinkedList<Type>();
			ts.add(new NamedType(typevar));
			constructors.add(new Constructor(tag, ts));

			final List<Expression> es = new LinkedList<Expression>();
			es.add(F);
			final Expression competitor = new Call(new Name(tag), es);
			competitors = new Parallel(competitor, competitors);

			final List<Pattern> ps = new LinkedList<Pattern>();
			ps.add(p);
			final Expression consequent = new Sequential(new Name(temp), G, new CallPattern(tag, ps));
			consequents = new Parallel(consequent, consequents);
		}

		Expression body = consequents;
		body = new Declare(new ValDeclaration(new VariablePattern(temp), competitors), body);
		body = new Declare(new DatatypeDeclaration(branch, constructors, typevars), body);

		return new WithLocation(body.simplify(), getSourceLocation());
	}

	@Override
	public String toString() {
		return "(" + join(choices, " ++ ") + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
