package orc.ast.simple;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Let extends Expression {

	public List<Argument> args;
	
	public Let(List<Argument> args)
	{
		this.args = args;
	}
	
	/* Special constructor for singleton */
	public Let(Argument arg)
	{
		this.args = new LinkedList<Argument>(); 
		this.args.add(arg);
	}
	
	/* Special constructor for empty let */
	public Let()
	{
		this.args = new LinkedList<Argument>();
	}
	
	@Override
	public Node compile(Node output) {
		orc.ast.simple.arg.Site let = new orc.ast.simple.arg.Site(new orc.runtime.sites.core.Let());
		return new orc.runtime.nodes.Call(let, args, output);
	}

	@Override
	public Expression subst(Argument a, NamedVar x) {
		List<Argument> newargs = new LinkedList<Argument>();		
		for (Argument b : args) {
			newargs.add(b.subst(a, x));
		}
		return new Let(newargs);
	}
	
	public Set<Var> vars() {
		Set<Var> freeset = new HashSet<Var>();
		for(Argument a : args)	{
			a.addFree(freeset);
		}
		return freeset;
	}
}