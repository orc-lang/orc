package orc.ast.simple;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.FreeVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Def extends Expression {

	public List<Definition> defs;
	public Expression body;
	
	public Def(List<Definition> defs, Expression body)
	{
		this.defs = defs;
		this.body = body;
	}
	
	@Override
	public Node compile(Node output) {
		
		List<orc.runtime.nodes.Definition> newdefs = new LinkedList<orc.runtime.nodes.Definition>();
		Node newbody = body.compile(output);
		
		Set<Var> freeset = new HashSet<Var>();
		for(Definition d : defs)
		{
			newdefs.add(d.compile());
			freeset.addAll(d.vars());
		}
		
		return new orc.runtime.nodes.Def(newdefs, newbody, freeset);
	}

	@Override
	public void subst(Argument a, FreeVar x) {
		
		for (Definition d : defs)
		{
			d.subst(a,x);
		}
		
		body.subst(a,x);
	}

	@Override
	public Set<Var> vars() {
		Set<Var> freeset = body.vars();
		for (Definition d : defs)
		{
			freeset.addAll(d.vars());
			freeset.remove(d.name);
		}
		
		return freeset;
	}

	
}
