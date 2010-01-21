//
// Call.java -- Java class Call
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
import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.inference.Constraint;
import orc.type.inference.InferenceRequest;
import orc.type.structured.ArrowType;
import orc.type.tycon.Variance;

public class Call extends Expression {

	public Argument callee;
	public List<Argument> args;
	public List<orc.ast.oil.type.Type> typeArgs; /* may be null to request inference */
	public boolean isTailCall = false;
	
	public Call(final Argument callee, final List<Argument> args, final List<orc.ast.oil.type.Type> typeArgs) {
		this.callee = callee;
		this.args = args;
		this.typeArgs = typeArgs;
	}

	/* Binary call constructor */
	public Call(final Argument callee, final Argument arga, final Argument argb) {
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arga);
		this.args.add(argb);
	}

	/* Unary call constructor */
	public Call(final Argument callee, final Argument arg) {
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arg);
	}

	/* Nullary call constructor */
	public Call(final Argument callee) {
		this.callee = callee;
		this.args = new LinkedList<Argument>();
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {

		callee.addIndices(indices, depth);
		for (final Argument arg : args) {
			arg.addIndices(indices, depth);
		}
	}

	@Override
	public String toString() {

		String arglist = " ";
		for (final Argument a : args) {
			arglist += a + " ";
		}

		return callee.toString() + "(" + arglist + ")";
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	public <E, C> E accept(final ContextualVisitor<E, C> cvisitor, final C initialContext) {
		return cvisitor.visit(this, initialContext);
	}

	/* Try to call the type without type argument synthesis */
	public Type noInferenceTypeSynth(final TypingContext ctx) throws TypeException {

		final Type calleeType = callee.typesynth(ctx);

		List<Type> typeActuals = null;

		if (typeArgs != null) {
			typeActuals = new LinkedList<Type>();
			for (final orc.ast.oil.type.Type t : typeArgs) {
				typeActuals.add(ctx.promote(t));
			}
		}

		/* Delegate the checking of the args and the call itself to the callee type.
		 * Some types do it differently than ArrowType does.
		 */
		return calleeType.call(ctx, args, typeActuals);

	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {

		/* Try to type the call */
		try {
			final Type S = noInferenceTypeSynth(ctx);

			/* If typing succeeded with null type parameters,
			 * set the type parameters to be an empty list,
			 * in case some other call might try to infer them
			 * as non-empty.
			 */
			if (typeArgs == null) {
				typeArgs = new LinkedList<orc.ast.oil.type.Type>();
			}

			return S;
		}
		/* If type parameter inference is needed, this exception will be thrown */
		catch (final InferenceRequest ir) {
			final ArrowType arrow = ir.requestedType;

			/* Arity check */
			if (args.size() != arrow.argTypes.size()) {
				throw new ArgumentArityException(arrow.argTypes.size(), args.size());
			}

			final Constraint[] C = new Constraint[arrow.typeArity];
			Env<Boolean> VX = new Env<Boolean>();

			for (int i = 0; i < arrow.typeArity; i++) {
				VX = VX.extend(false);
				C[i] = new Constraint();
			}

			/* Add constraints for the argument types */
			for (int i = 0; i < args.size(); i++) {
				final Type A = args.get(i).typesynth(ctx);
				final Type B = arrow.argTypes.get(i);

				A.addConstraints(VX, B, C);
			}

			/*
			for (Constraint c : C) {
				System.out.println(c);
			}
			*/

			/* Find type arguments that minimize the result type */
			final List<Type> inferredTypeArgs = new LinkedList<Type>();
			Env<Type> subs = new Env<Type>();
			final Type R = arrow.resultType;
			for (int i = arrow.typeArity - 1; i >= 0; i--) {
				final Type sigmaCR = C[i].minimal(R.findVariance(i));
				inferredTypeArgs.add(sigmaCR);
				subs = subs.extend(sigmaCR);
			}

			/* We have successfully inferred the type arguments */
			// FIXME
			//typeArgs = inferredTypeArgs;

			return R.subst(subs);
		}

	}

	@Override
	public void typecheck(final TypingContext ctx, final Type T) throws TypeException {

		/* Try to type the call */
		try {
			final Type S = noInferenceTypeSynth(ctx);
			S.assertSubtype(T);

			/* If typing succeeded without any type parameters,
			 * set the type parameters to be an empty list,
			 * in case some other call might try to infer them
			 * as non-empty.
			 */
			if (typeArgs == null) {
				typeArgs = new LinkedList<orc.ast.oil.type.Type>();
			}
			return;
		}
		/* If type parameter inference is needed, this exception will be thrown */
		catch (final InferenceRequest ir) {
			final ArrowType arrow = ir.requestedType;

			/* Arity check */
			if (args.size() != arrow.argTypes.size()) {
				throw new ArgumentArityException(arrow.argTypes.size(), args.size());
			}

			final Constraint[] C = new Constraint[arrow.typeArity];
			Env<Boolean> VX = new Env<Boolean>();

			for (int i = 0; i < arrow.typeArity; i++) {
				VX = VX.extend(false);
				C[i] = new Constraint();
			}

			/* Add constraints for the argument types */
			for (int i = 0; i < args.size(); i++) {
				final Type A = args.get(i).typesynth(ctx);
				final Type B = arrow.argTypes.get(i);
				A.addConstraints(VX, B, C);
			}

			/* Add constraints for the result type */
			arrow.resultType.addConstraints(VX, T, C);

			final List<Type> inferredTypeArgs = new LinkedList<Type>();
			/* Find (any) type arguments permitted by the constraints C */
			for (final Constraint c : C) {
				inferredTypeArgs.add(0, c.minimal(Variance.COVARIANT));
			}

			/* We have successfully inferred the type arguments */
			// FIXME
			//typeArgs = inferredTypeArgs;
		}
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		final LinkedList<orc.ast.xml.expression.argument.Argument> arguments = new LinkedList<orc.ast.xml.expression.argument.Argument>();
		for (final orc.ast.oil.expression.argument.Argument a : args) {
			arguments.add(a.marshal());
		}
		return new orc.ast.xml.expression.Call(callee.marshal(), arguments.toArray(new orc.ast.xml.expression.argument.Argument[] {}), orc.ast.oil.type.Type.marshalAll(typeArgs));
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		// No children
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		try {
			final Callable target = Value.forceCall(t.lookup(callee), t);

			/** 
			 * target is null if the callee is still unbound, in which
			 * case the calling token will be activated when the
			 * callee value becomes available. Thus, we simply
			 * return and wait for the token to enter the process
			 * method again.
			 */
			if (target == Value.futureNotReady) {
				return;
			}

			/**
			 * Collect all of the environment's bindings for these args.
			 * Note that some of them may still be unbound, since we are
			 * not forcing the futures.
			 */
			final List<Object> actuals = new LinkedList<Object>();

			for (final Argument a : args) {
				actuals.add(t.lookup(a));
			}

			target.createCall(t, actuals, getPublishContinuation());

		} catch (final TokenException e) {
			// if uncaught,  t.error(e) will be called.
			t.throwRuntimeException(e);
		}
	}
}
