package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.oil.expression.Expr;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
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
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Parallel(left.convert(vars, typevars), right.convert(vars, typevars));
	}

}
