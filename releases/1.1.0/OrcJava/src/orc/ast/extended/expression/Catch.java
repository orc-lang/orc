//
// Catch.java -- Java class Catch
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import orc.ast.extended.declaration.def.Clause;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.TypeVariable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public class Catch extends Expression {
	public List<CatchHandler> handlers;
	public Expression tryBlock;

	protected List<String> typeParams;
	protected List<Type> argTypes;
	protected Type resultType;

	public Catch(final Expression tryBlock, final List<CatchHandler> handlers) {
		this.tryBlock = tryBlock;
		this.handlers = handlers;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {

		final Variable e = new Variable();
		final List<Variable> formals = new ArrayList<Variable>();
		formals.add(e);

		/*
		 * Handle the case where none of the handlers match, in which case we need to rethrow.
		 */
		final List<Pattern> rethrowFormals = new ArrayList<Pattern>();
		final VariablePattern exceptionPattern = new VariablePattern("e");
		rethrowFormals.add(exceptionPattern);
		final Expression rethrowBody = new Name("e");
		rethrowBody.setSourceLocation(getSourceLocation());
		final orc.ast.extended.expression.Throw rethrow = new orc.ast.extended.expression.Throw(rethrowBody);
		rethrow.setSourceLocation(getSourceLocation());
		final Clause rethrowClause = new Clause(rethrowFormals, rethrow);

		final orc.ast.simple.expression.Expression fail = Pattern.fail();
		orc.ast.simple.expression.Expression body = rethrowClause.simplify(formals, fail);

		/*
		 * Simplify the list of exception handlers
		 */
		Collections.reverse(handlers);
		for (final CatchHandler c : handlers) {
			final Clause handlerClause = new Clause(c.catchPattern, c.body);
			body = handlerClause.simplify(formals, body);
		}

		final orc.ast.simple.expression.Expression simpleTryBlock = tryBlock.simplify();
		final Variable unnamedVar = new Variable();
		final List<TypeVariable> typeParams = new ArrayList<TypeVariable>();
		final SourceLocation sourceLocation = getSourceLocation();
		final orc.ast.simple.type.Type resultType = new orc.ast.simple.type.Bot();
		final orc.ast.simple.expression.Def def = new orc.ast.simple.expression.Def(unnamedVar, formals, body, typeParams, null, resultType, sourceLocation);
		return new orc.ast.simple.expression.Catch(def, simpleTryBlock);
	}

	@Override
	public String toString() {
		String s = "try (" + tryBlock.toString() + ")";
		for (final CatchHandler c : handlers) {
			s = s + c.toString();
		}
		return s;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
