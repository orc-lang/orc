package orc.ast.oil.expression;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.oil.expression.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
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
	
	public Call(Argument callee, List<Argument> args, List<orc.ast.oil.type.Type> typeArgs)
	{
		this.callee = callee;
		this.args = args;
		this.typeArgs = typeArgs;
	}
	
	/* Binary call constructor */
	public Call(Argument callee, Argument arga, Argument argb)
	{
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arga);
		this.args.add(argb);
	}
	
	/* Unary call constructor */
	public Call(Argument callee, Argument arg)
	{
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arg);
	}
	
	/* Nullary call constructor */
	public Call(Argument callee)
	{
		this.callee = callee;
		this.args = new LinkedList<Argument>();
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		
		callee.addIndices(indices, depth);
		for (Argument arg : args) {
			arg.addIndices(indices, depth);
		}
	}
	
	public String toString() {
		
		String arglist = " ";
		for (Argument a : args) {
			arglist += a + " ";
		}
	
		return callee.toString() + "(" + arglist + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}


	/* Try to call the type without type argument synthesis */
	public Type noInferenceTypeSynth(TypingContext ctx) throws TypeException {
		
		Type calleeType = callee.typesynth(ctx);
		
		List<Type> typeActuals = null;
		
		if (typeArgs != null) {
			typeActuals = new LinkedList<Type>();
			for (orc.ast.oil.type.Type t : typeArgs) {
				typeActuals.add(ctx.promote(t));
			}
		}
		
		/* Delegate the checking of the args and the call itself to the callee type.
		 * Some types do it differently than ArrowType does.
		 */
		return calleeType.call(ctx, args, typeActuals);
		
	}
	
	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		
		/* Try to type the call */
		try {
			Type S = noInferenceTypeSynth(ctx);
			
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
		catch (InferenceRequest ir) {
			ArrowType arrow = ir.requestedType;
		
			/* Arity check */
			if (args.size() != arrow.argTypes.size()) {
				throw new ArgumentArityException(arrow.argTypes.size(), args.size());
			}
				
			Constraint[] C = new Constraint[arrow.typeArity];
			Env<Boolean> VX = new Env<Boolean>();
				
			for (int i = 0; i < arrow.typeArity; i++) {
				VX = VX.extend(false);
				C[i] = new Constraint();
			}
				
				
			/* Add constraints for the argument types */
			for (int i = 0; i < args.size(); i++) {
				Type A = args.get(i).typesynth(ctx);
				Type B = arrow.argTypes.get(i);
				
				A.addConstraints(VX, B, C);
			}
			
			/*
			for (Constraint c : C) {
				System.out.println(c);
			}
			*/
			
			/* Find type arguments that minimize the result type */
			List<Type> inferredTypeArgs = new LinkedList<Type>();
			Env<Type> subs = new Env<Type>();
			Type R = arrow.resultType;
			for (int i = arrow.typeArity - 1; i >= 0; i--) {
				Type sigmaCR = C[i].minimal(R.findVariance(i));
				inferredTypeArgs.add(sigmaCR);
				subs = subs.extend(sigmaCR);
			}
				
			/* We have successfully inferred the type arguments */
			// FIXME
			//typeArgs = inferredTypeArgs;
				
			return R.subst(subs);	
		}
		
	}
	
	public void typecheck(TypingContext ctx, Type T) throws TypeException {
		
		/* Try to type the call */
		try {
			Type S = noInferenceTypeSynth(ctx);			
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
		catch (InferenceRequest ir) {
			ArrowType arrow = ir.requestedType;
				
				/* Arity check */
				if (args.size() != arrow.argTypes.size()) {
					throw new ArgumentArityException(arrow.argTypes.size(), args.size());
				}
				
				Constraint[] C = new Constraint[arrow.typeArity];
				Env<Boolean> VX = new Env<Boolean>();
				
				for (int i = 0; i < arrow.typeArity; i++) {
					VX = VX.extend(false);
					C[i] = new Constraint();
				}
				
				/* Add constraints for the argument types */
				for (int i = 0; i < args.size(); i++) {
					Type A = args.get(i).typesynth(ctx);
					Type B = arrow.argTypes.get(i);
					A.addConstraints(VX, B, C);
				}
				
				/* Add constraints for the result type */
				arrow.resultType.addConstraints(VX, T, C);
				
				List<Type> inferredTypeArgs = new LinkedList<Type>();
				/* Find (any) type arguments permitted by the constraints C */
				for (Constraint c : C) {
					inferredTypeArgs.add(0, c.minimal(Variance.COVARIANT));
				}
				
				/* We have successfully inferred the type arguments */
				// FIXME
				//typeArgs = inferredTypeArgs;
			}
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		LinkedList<orc.ast.xml.expression.argument.Argument> arguments
			= new LinkedList<orc.ast.xml.expression.argument.Argument>();
		for (orc.ast.oil.expression.argument.Argument a : args) {
			arguments.add(a.marshal());
		}
		return new orc.ast.xml.expression.Call(callee.marshal(),
				arguments.toArray(new orc.ast.xml.expression.argument.Argument[]{}),
				orc.ast.oil.type.Type.marshalAll(typeArgs));
	}
}
