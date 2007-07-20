package orc.ast.simple;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.FreeVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Call extends Expression {

	public Argument caller;
	public List<Argument> args;
	
	public Call(Argument caller, List<Argument> args)
	{
		this.caller = caller;
		this.args = args;
	}
	
	/* Nullary call constructor */
	public Call(Argument caller)
	{
		this.caller = caller;
		this.args = new LinkedList<Argument>();
	}

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Call(caller, args, output);
	}

	@Override
	public void subst(Argument a, FreeVar x) {
		
		// Substitute on the call target
		if (caller.equals(x))
		{
			caller = a;
		}
		
		// Substitute on the arguments
		List<Argument> newargs = new LinkedList<Argument>();
		
		for (Argument b : args)
		{
			if (b.equals(x))
				newargs.add(a);
			else
				newargs.add(b);
		}
		args = newargs;
		
		
	}

	public Set<Var> vars()
	{
		Set<Var> freeset = new HashSet<Var>();
		
		if (caller instanceof Var)
		{ freeset.add((Var)caller); }
		
		for(Argument a : args)
		{
			if (a instanceof Var)
			{
				freeset.add((Var)a);
			}
		}
		
		return freeset;
	}
	
}
