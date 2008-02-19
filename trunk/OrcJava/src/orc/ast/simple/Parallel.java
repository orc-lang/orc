package orc.ast.simple;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;

public class Parallel extends Expression {

	Expression left;
	Expression right;
	
	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return new Fork(left.compile(output), right.compile(output));
	}

	@Override
	public Expression subst(Argument a, NamedVar x) 
	{
		return new Parallel(left.subst(a,x),right.subst(a,x));
	}

	@Override
	public Set<Var> vars() {
		
		Set<Var> s = left.vars();
		s.addAll(right.vars());
		return s;
	}

}
