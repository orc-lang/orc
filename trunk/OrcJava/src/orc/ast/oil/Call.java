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
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.ArrowType;
import orc.type.Type;

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
				// TODO: Type argument inference unimplemented
				throw new TypeException("Type argument inference unimplemented.");
			}
			else {
				/* Otherwise set type arguments to an empty list and try it again from the top */
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
				// TODO: Type argument inference unimplemented
				throw new TypeException("Type argument inference unimplemented.");
			}
			else {
				/* Otherwise set type arguments to an empty list and try it again from the top */
				typeArgs = new LinkedList<Type>();
				typecheck(T, ctx, typectx);
			}
		}
	}
	
}
