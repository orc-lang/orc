package orc.ast.simple;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.FreeVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;

public class Where extends Expression {

	Expression left;
	Expression right;
	Var v;
	
	public Where(Expression left, Expression right, Var v)
	{
		this.left = left;
		this.right = right;
		this.v = v;
	}
	
	@Override
	public Node compile(Node output) {	
		return new orc.runtime.nodes.Where(left.compile(output), v, right.compile(new Store(v)));
	}

	@Override
	public void subst(Argument a, FreeVar x) 
	{
		left.subst(a,x);
		right.subst(a,x);
	}

	public Set<Var> vars() {
		
		Set<Var> s = left.vars();
		s.addAll(right.vars());
		s.remove(v);
		return s;
	}
	
}
