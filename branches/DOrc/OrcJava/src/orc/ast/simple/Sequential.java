package orc.ast.simple;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Node;

public class Sequential extends Expression {

	Expression left;
	Expression right;
	Var v;
	
	public Sequential(Expression left, Expression right, Var v)
	{
		this.left = left;
		this.right = right;
		this.v = v;
	}
	
	@Override
	public Node compile(Node output) {
		return left.compile(new Assign(v, right.compile(output)));
	}

	@Override
	public Sequential subst(Argument a, NamedVar x) 
	{
		return new Sequential(left.subst(a,x), right.subst(a,x), v);
	}
	
	public Set<Var> vars() {
		
		Set<Var> s = left.vars();
		s.addAll(right.vars());
		s.remove(v);
		return s;
	}

}
