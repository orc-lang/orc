package orc.ast.oil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Unwind;

public class Defs extends Expr {

	public List<Def> defs;
	public Expr body;
	
	public Defs(List<Def> defs, Expr body)
	{
		this.defs = defs;
		this.body = body;
	}
	
	@Override
	public Node compile(Node output) {
		
		List<orc.runtime.nodes.Def> newdefs = new LinkedList<orc.runtime.nodes.Def>();
		
		for(Def d : defs)
		{
			newdefs.add(d.compile());	
		}
		
		Node newbody = body.compile(new Unwind(output, newdefs.size()));
				
		return new orc.runtime.nodes.Defs(newdefs, newbody, this.freeVars());
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		
		for (Def d : defs) {
			d.addVars(indices, depth + defs.size());
		}
	}
	
	public String toString() {
		
		String repn = "(defs  ";
		for (Def d : defs) {
			repn += "\n  " + d.toString();
		}
		repn += "\n)\n" + body.toString();
		return repn;
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
