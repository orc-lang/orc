package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.Bar;
import orc.ast.oil.Expr;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Parallel extends Expression {

	Expression left;
	Expression right;
	
	public Parallel(Expression left, Expression right)
	{
		this.left = left;
		this.right = right;
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

	@Override
	public Expr convert(Env<Var> vars) throws CompilationException {
		return new Bar(left.convert(vars), right.convert(vars));
	}

}
