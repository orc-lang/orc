//
// DeclareDefs.java -- Java class DeclareDefs
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.oil.expression.argument.Variable;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnspecifiedReturnTypeException;
import orc.runtime.Token;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.structured.ArrowType;

public class DeclareDefs extends Expression {

	public List<Def> defs;
	public Expression body;

	public DeclareDefs(final List<Def> defs, final Expression body) {
		this.defs = defs;
		this.body = body;
	}

	@Override
	public void addIndices(final Set<Integer> indices, int depth) {
		depth += defs.size();
		for (final Def d : defs) {
			d.addIndices(indices, depth);
		}
		body.addIndices(indices, depth);
	}

	@Override
	public String toString() {
		String repn = "(defs  ";
		for (final Def d : defs) {
			repn += "\n  " + d.toString();
		}
		repn += "\n)\n" + body.toString();
		return repn;
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

		/* This is the typing context we will use to check the scoped expression */
		TypingContext dctx = ctx;

		/* Add the types for each definition in the group to the context */
		for (final Def d : defs) {
			try {
				dctx = dctx.bindVar(d.type(ctx));
			}
			/* If the return type is unspecified, this function can't be called recursively,
			 * so its name cannot have a type associated with it; use null instead.
			 */
			catch (final UnspecifiedReturnTypeException e) {
				dctx = dctx.bindVar(null);
			} catch (final TypeException e) {
				e.setSourceLocation(d.getSourceLocation());
				throw e;
			}
		}

		/* 
		 * Use this context, with all definition names bound,
		 * to verify each definition individually.
		 */
		for (final Def d : defs) {
			d.checkDef(dctx);
		}

		/* Now, repeat the process, but require each definition to provide a type.
		 * Any missing return type ascriptions should now be filled in.
		 */
		TypingContext bodyctx = ctx;
		for (final Def d : defs) {
			bodyctx = bodyctx.bindVar(d.type(ctx));
		}

		/*
		 * The synthesized type of the body in this context is
		 * the synthesized type for the whole expression.
		 */
		return body.typesynth(bodyctx);
	}

	/* There is a special case when checking translated lambdas, so we override this method */
	@Override
	public void typecheck(TypingContext ctx, final Type T) throws TypeException {

		/* Check whether this definition group is a translated lambda,
		 * and make sure that the type being checked is an arrow type.
		 */
		if (defs.size() == 1 && body.getClass().equals(Variable.class) && ((Variable) body).index == 0 && T instanceof ArrowType) {
			/* Add an empty mapping for the type of the function itself;
			 * since it is anonymous, recursion can never occur.
			 */
			ctx = ctx.bindVar(null);

			defs.get(0).checkLambda(ctx, (ArrowType) T);
		} else {
			/* Otherwise, perform checking as usual */
			super.typecheck(ctx, T);
		}
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		final LinkedList<orc.ast.xml.expression.Def> definitions = new LinkedList<orc.ast.xml.expression.Def>();
		for (final Def d : defs) {
			definitions.add(d.marshal());
		}
		return new orc.ast.xml.expression.DeclareDefs(definitions.toArray(new orc.ast.xml.expression.Def[] {}), body.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		body.setPublishContinuation(getPublishContinuation());
		for (final Def def : defs) {
			def.populateContinuations();
		}
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
