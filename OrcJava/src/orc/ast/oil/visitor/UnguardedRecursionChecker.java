//
// UnguardedRecursionChecker.java -- Java class UnguardedRecursionChecker
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

package orc.ast.oil.visitor;

import java.util.LinkedList;

import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Variable;
import orc.env.Env;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * Check for unguarded recursion in function definitions.
 * TODO: check for unguarded mutual recursion as well.
 * @author quark
 */
public class UnguardedRecursionChecker extends Walker {
	public static void check(final Expression expr) throws CompilationException {
		final UnguardedRecursionChecker checker = new UnguardedRecursionChecker();
		expr.accept(checker);
		if (checker.problems.size() > 0) {
			throw checker.problems.getFirst();
		}
	}

	/** A binding is true if it is unguarded in the current scope. */
	private Env<Boolean> env = new Env<Boolean>();
	/** Accumulate problems. */
	private final LinkedList<CompilationException> problems = new LinkedList<CompilationException>();
	private SourceLocation location;

	private UnguardedRecursionChecker() {
	}

	@Override
	public Void visit(final DeclareDefs expr) {
		// save the environment
		final Env<Boolean> outerEnv = env.clone();
		final int ndefs = expr.defs.size();
		int whichdef = 0;
		// check each def in turn
		for (final Def def : expr.defs) {
			// FIXME: this copies the environment more often than strictly necessary
			env = envForDef(ndefs, whichdef, def.arity, outerEnv);
			def.body.accept(this);
			++whichdef;
		}
		// check the body
		env = outerEnv.clone();
		for (int i = 0; i < ndefs; ++i) {
			env.add(false);
		}
		expr.body.accept(this);
		// restore the environment
		env = outerEnv;
		return null;
	}

	@Override
	public Void visit(final Pruning expr) {
		// The pull adds a binding to the LHS,
		// but we don't care what it is.
		env.add(false);
		expr.left.accept(this);
		env.unwind(1);

		expr.right.accept(this);
		return null;
	}

	@Override
	public Void visit(final Sequential expr) {
		expr.left.accept(this);

		// The push adds a binding to the RHS,
		// and changes all previous bindings to
		// false within the RHS
		final Env<Boolean> outerEnv = env;
		env = envForPush(outerEnv);
		expr.right.accept(this);
		env = outerEnv;
		return null;
	}

	@Override
	public Void visit(final Otherwise expr) {
		expr.left.accept(this);

		// The semi changes all previous bindings to
		// false within the RHS
		final Env<Boolean> outerEnv = env;
		env = envForPush(outerEnv);
		expr.right.accept(this);
		env = outerEnv;
		return null;
	}

	@Override
	public Void visit(final Call expr) {
		// dispatch on the type of the callee
		expr.callee.accept(this);
		// unguarded recursion in call arguments is ok
		return null;
	}

	@Override
	public Void visit(final Variable arg) {
		// check whether the var refers to an unguarded outer
		// definition in the environment
		if (arg.resolveGeneric(env)) {
			final CompilationException e = new CompilationException("Unguarded recursion found.");
			e.setSourceLocation(location);
			problems.add(e);
		}
		return null;
	}

	@Override
	public Void visit(final WithLocation expr) {
		// while we are visiting the annotated expression,
		// we need to use the given location for error messages
		final SourceLocation outerLocation = location;
		location = expr.location;
		super.visit(expr);
		location = outerLocation;
		return null;
	}

	@Override
	public Void visit(final Throw throwExpr) {
		throwExpr.exception.accept(this);
		return null;
	}

	@Override
	public Void visit(final Catch catchExpr) {
		catchExpr.tryBlock.accept(this);
		catchExpr.handler.body.accept(this);
		return null;
	}

	/**
	 * Generate a new environment for checking the body of a def. Specifically,
	 * the value for this def's binding is true, and for all sibling defs it's false.
	 */
	private static Env<Boolean> envForDef(final int ndefs, final int whichdef, final int arity, final Env<Boolean> env) {
		final Env<Boolean> out = env.clone();
		for (int i = 0; i < whichdef; ++i) {
			out.add(false);
		}
		out.add(true);
		for (int i = whichdef + 1; i < ndefs; ++i) {
			out.add(false);
		}
		for (int i = 0; i < arity; ++i) {
			out.add(false);
		}
		return out;
	}

	/**
	 * Generate an environment for checking the RHS of a semi.
	 * Specifically, all bindings are false (since they are guarded by the semi).
	 */
	private static Env<Boolean> envForSemi(final Env<Boolean> env) {
		final Env<Boolean> out = new Env<Boolean>();
		for (@SuppressWarnings("unused")
		final Boolean _ : env.items()) {
			out.add(false);
		}
		return out;
	}

	/**
	 * Generate an environment for checking the RHS of a push.
	 * Specifically, all bindings are false (since they are guarded by the push).
	 */
	private static Env<Boolean> envForPush(final Env<Boolean> env) {
		final Env<Boolean> out = envForSemi(env);
		// plus one binding for the push itself
		out.add(false);
		return out;
	}

}
