package orc.ast.simple;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Semi extends Expression {

	Expression left;
	Expression right;
	
	public Semi(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Semi(left.compile(output), right.compile(output));
	}

	@Override
	public Expression subst(Argument a, NamedVar x) 
	{
		return new Semi(left.subst(a,x),right.subst(a,x));
	}

	@Override
	public Set<Var> vars() {
		
		Set<Var> s = left.vars();
		s.addAll(right.vars());
		return s;
	}

}
