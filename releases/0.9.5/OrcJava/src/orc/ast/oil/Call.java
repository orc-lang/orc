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
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.type.Type;

public class Call extends Expr {

	public Arg callee;
	public List<Arg> args;
	
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


	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
		
		Type S = callee.typesynth(ctx);
		List<Type> T = new LinkedList<Type>();
		
		for (Arg a : args) {
			T.add(a.typesynth(ctx));
		}
		
		return S.call(T);
	}
	
}
