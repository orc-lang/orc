package orc.ast.simple;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Call extends Expression {

	public Argument callee;
	public List<Argument> args;
	
	public Call(Argument callee, List<Argument> args)
	{
		this.callee = callee;
		this.args = args;
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
	public Node compile(Node output) {
		orc.runtime.nodes.Call c = new orc.runtime.nodes.Call(callee, args, output);
		c.setDebugInfo(this);
		return c;
	}

	@Override
	public Expression subst(Argument a, NamedVar x) {
		List<Argument> newargs = new LinkedList<Argument>();
		for (Argument b : args)	{
			newargs.add(b.subst(a, x));
		}
		return new Call(callee.subst(a, x), newargs);
	}

	public Set<Var> vars() {
		Set<Var> freeset = new HashSet<Var>();
		callee.addFree(freeset);
		for(Argument a : args) {
			a.addFree(freeset);
		}
		return freeset;
	}
}
