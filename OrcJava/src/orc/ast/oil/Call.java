package orc.ast.oil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.arg.Arg;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.ArrowType;
import orc.type.Constraint;
import orc.type.Type;
import orc.type.Variance;

public class Call extends Expr {

	public Arg callee;
	public List<Arg> args;
	public List<Type> typeArgs; /* may be null to request inference */
	
	public Call(Arg callee, List<Arg> args, List<Type> typeArgs)
	{
		this.callee = callee;
		this.args = args;
		this.typeArgs = typeArgs;
	}
	
	public Call(Arg callee, List<Arg> args)
	{
		this.callee = callee;
		this.args = args;
	}
	
	/* Binary call constructor */
	public Call(Arg callee, Arg arga, Arg argb)
	{
		this.callee = callee;
		this.args = new LinkedList<Arg>();
		this.args.add(arga);
		this.args.add(argb);
	}
	
	/* Unary call constructor */
	public Call(Arg callee, Arg arg)
	{
		this.callee = callee;
		this.args = new LinkedList<Arg>();
		this.args.add(arg);
	}
	
	/* Nullary call constructor */
	public Call(Arg callee)
	{
		this.callee = callee;
		this.args = new LinkedList<Arg>();
	}
	

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Call(callee, args, output);
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		
		callee.addIndices(indices, depth);
		for (Arg arg : args) {
			arg.addIndices(indices, depth);
		}
	}
	
	public String toString() {
		
		String arglist = " ";
		for (Arg a : args) {
			arglist += a + " ";
		}
	
		return callee.toString() + "(" + arglist + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}


	/* Type synthesis when inference of type parameters is not required. */
	public Type noInferenceTypeSynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		Type calleeType = callee.typesynth(ctx, typectx);
		
		List<Type> typeActuals = new LinkedList<Type>();
		for (Type t : typeArgs) {
			typeActuals.add(t.subst(typectx));
		}
		
		/* Delegate the checking of the args and the call itself to the callee type.
		 * Some types do it differently than ArrowType does.
		 */
		return calleeType.call(ctx, typectx, args, typeActuals);
		
	}
	
	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		if (typeArgs != null) {
			Type S = noInferenceTypeSynth(ctx, typectx);
			return S;
		}
		/* Infer type arguments */
		else {
			Type calleeType = callee.typesynth(ctx, typectx);
		
			if (calleeType instanceof ArrowType && ((ArrowType)calleeType).typeArity > 0) {
				
				ArrowType arrow = (ArrowType)calleeType;
				
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
					Type A = args.get(i).typesynth(ctx, typectx);
					Type B = arrow.argTypes.get(i);
					
					/*
					for (Constraint c : C) {
						System.out.println(c);
					}
					
					System.out.println("A: " + A + " and B: " + B);
					*/
					
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
				typeArgs = inferredTypeArgs;
				
				return R.subst(subs);
			}
			else {
				/* Type arity is 0; no arguments to infer */
				/* Set type arguments to an empty list and try again from the top */
				typeArgs = new LinkedList<Type>();
				return typesynth(ctx, typectx);
			}
		}
		
	}
	
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		if (typeArgs != null) {
			Type S = noInferenceTypeSynth(ctx, typectx);
			if (!S.subtype(T)) {
				throw new SubtypeFailureException(S,T);
			}
		}
		/* Infer type arguments */
		else {
			Type calleeType = callee.typesynth(ctx, typectx);
			
			if (calleeType instanceof ArrowType && ((ArrowType)calleeType).typeArity > 0) {
				
				ArrowType arrow = (ArrowType)calleeType;
				
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
					Type A = args.get(i).typesynth(ctx, typectx);
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
				typeArgs = inferredTypeArgs;
			}
			else {
				/* Type arity is 0; no arguments to infer */
				/* Set type arguments to an empty list and try again from the top */
				typeArgs = new LinkedList<Type>();
				typecheck(T, ctx, typectx);
			}
		}
	}
	
	
}
