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
import java.util.TreeSet;

import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.visitor.Visitor;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnspecifiedReturnTypeException;
import orc.runtime.Token;
import orc.runtime.values.Closure;
import orc.runtime.values.Future;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.structured.ArrowType;

/**
 * 
 * A group of mutually recursive definitions, 
 * and the expression to which they are scoped.
 *
 * @author dkitchin
 */
public class DeclareDefs extends Expression {

	public List<Def> defs;
	public Expression body;

	/**
	 * Variables defined outside this node which appear in the bodies of
	 * the defs. If the defs are all mutually recursive, this is correct,
	 * otherwise this is overly pessimistic and may force some defs to wait
	 * on variables which they won't use.
	 * 
	 * The compiler ensures that all def groups are mutually recursive
	 * at this point in compilation.
	 */
	public Set<Variable> free;

	public DeclareDefs(final List<Def> defs, final Expression body) {
		this.defs = defs;
		this.body = body;

		/* Compute the set of free variables for this group of defs,
		 * which will be closed over when creating closures.
		 */
		free = new TreeSet<Variable>();
		final Set<Integer> indices = new TreeSet<Integer>();
		final int depth = defs.size();
		for (final Def d : defs) {
			d.addIndices(indices, depth);
		}
		for (final Integer i : indices) {
			free.add(new Variable(i));
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (body == null ? 0 : body.hashCode());
		result = prime * result + (defs == null ? 0 : defs.hashCode());
		result = prime * result + (free == null ? 0 : free.hashCode());
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
		final DeclareDefs other = (DeclareDefs) obj;
		if (body == null) {
			if (other.body != null) {
				return false;
			}
		} else if (!body.equals(other.body)) {
			return false;
		}
		if (defs == null) {
			if (other.defs != null) {
				return false;
			}
		} else if (!defs.equals(other.defs)) {
			return false;
		}
		if (free == null) {
			if (other.free != null) {
				return false;
			}
		} else if (!free.equals(other.free)) {
			return false;
		}
		return true;
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
		// Trigger a NullPointerException if this node's publish continutation is executed,
		// rather than its child (which would skip up the AST above this node)
		setPublishContinuation(null);
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
		// Step 0: find free values in the environment
		final List<Object> freeValues = new LinkedList<Object>();
		for (final Variable v : free) {
			final Object value = v.resolve(t.getEnvironment());
			if (value instanceof Future) {
				freeValues.add(value);
			}
		}

		// Step 1: create and bind the closures
		final Closure[] closures = new Closure[defs.size()];
		int i = 0;
		for (final Def d : defs) {
			t.bind(closures[i++] = new Closure(d, freeValues));
		}
		// Now the environment is correct relative to the body

		// Step 2: set the environment of each closure
		// These closures are compacted, which is why we
		// consult d.free
		i = 0;
		for (@SuppressWarnings("unused") final Def d : defs) {
			final Closure c = closures[i++];

			//FIXME: Add closure compaction back!
			/*
			 * alignment for closure compaction
			 */
			//Env<Object> env = new Env<Object>();
			//for (Variable v : d.free) env.add(v.resolve(t.getEnvironment()));
			//c.env = env;

			c.env = t.getEnvironment().clone();
		}

		body.enter(t.move(body));
	}
}
