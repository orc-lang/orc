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

import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.visitor.Visitor;
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
import orc.type.inference.InferenceContinuation;
import orc.type.structured.ArrowType;
import orc.type.tycon.Variance;

public class Call extends Expression {

	public Argument callee;
	public List<Argument> args;
	public List<orc.ast.oil.type.Type> typeArgs; /* may be null to request inference */
	transient public boolean isTailCall = false;
	
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (args == null ? 0 : args.hashCode());
		result = prime * result + (callee == null ? 0 : callee.hashCode());
		result = prime * result + (typeArgs == null ? 0 : typeArgs.hashCode());
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
		final Call other = (Call) obj;
		if (args == null) {
			if (other.args != null) {
				return false;
			}
		} else if (!args.equals(other.args)) {
			return false;
		}
		if (callee == null) {
			if (other.callee != null) {
				return false;
			}
		} else if (!callee.equals(other.callee)) {
			return false;
		}
		if (typeArgs == null) {
			if (other.typeArgs != null) {
				return false;
			}
		} else if (!typeArgs.equals(other.typeArgs)) {
			return false;
		}
		return true;
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

	public Type findReturnType(final TypingContext ctx, final Type checkedType) throws TypeException {

		final Type calleeType = callee.typesynth(ctx);

		if (typeArgs != null) {
			List<Type> typeActuals = new LinkedList<Type>();
			for (final orc.ast.oil.type.Type t : typeArgs) {
				typeActuals.add(ctx.promote(t));
			}
			/* Delegate the checking of the args and the call itself to the callee type.
			 * Some types do it differently than ArrowType does.
			 */
			return calleeType.call(ctx, args, typeActuals);
		}
		/* We may need to infer type arguments. */
		else {
			/* Create a continuation for requests to infer type arguments
			 * for this call.
			 */
			InferenceContinuation ic = new InferenceContinuation() {
				
				public Type inferFrom(ArrowType arrowType) throws TypeException { 

					List<Type> inferredTypeArgs = new LinkedList<Type>();
					
					/* Arity check */
					if (args.size() != arrowType.argTypes.size()) {
						throw new ArgumentArityException(arrowType.argTypes.size(), args.size());
					}
					
					Constraint[] C = new Constraint[arrowType.typeArity];
					Env<Boolean> VX = new Env<Boolean>();

					for (int i = 0; i < arrowType.typeArity; i++) {
						VX = VX.extend(false);
						C[i] = new Constraint();
					}

					/* Add constraints for the argument types */
					for (int i = 0; i < args.size(); i++) {
						final Type A = args.get(i).typesynth(ctx);
						final Type B = arrowType.argTypes.get(i);

						A.addConstraints(VX, B, C);
					}
					
					/* If we are in checking mode, use the checked
					 * type to make inference more precise.
					 */
					if (checkedType != null) {
						/* Add constraints for the result type */
						arrowType.resultType.addConstraints(VX, checkedType, C);

						/* Find (any) type arguments permitted by the constraints C */
						for (final Constraint c : C) {
							inferredTypeArgs.add(0, c.minimal(Variance.COVARIANT));
						}
						
						/* If inference has succeeded, just return the checked type */
						return checkedType;
					}
					/* Otherwise, we simply try to find a minimal type */
					else {
						/* Find type arguments that minimize the result type */
						Env<Type> subs = new Env<Type>();
						final Type R = arrowType.resultType;
						for (int i = arrowType.typeArity - 1; i >= 0; i--) {
							final Type sigmaCR = C[i].minimal(R.findVariance(i));
							inferredTypeArgs.add(sigmaCR);
							subs = subs.extend(sigmaCR);
						}
						
						
						
						return R.subst(subs);
					}
				}
			};
			/* end of continuation */
			
			/* FIXME:
			 * This implementation of inference has a weakness: it may be possible for
			 * different inference passes to infer different, incompatible type parameters.
			 * According to the theory, this is technically incorrect.
			 * However, in practice, it is very unlikely to occur.  
			 */
			
			/* Delegate the checking of the args and the call itself to the callee type.
			 * Some types do it differently than ArrowType does.
			 */
			return calleeType.call(ctx.bindIC(ic), args, null);
		}
		
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		return findReturnType(ctx, null);
	}

	@Override
	public void typecheck(final TypingContext ctx, final Type T) throws TypeException {
		findReturnType(ctx, T).assertSubtype(T);
		return;
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
