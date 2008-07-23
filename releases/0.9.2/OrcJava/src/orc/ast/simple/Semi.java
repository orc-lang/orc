package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.Expr;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
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


	@Override
	public Expr convert(Env<Var> vars) {
		return new orc.ast.oil.Semi(left.convert(vars), right.convert(vars));
	}

}
