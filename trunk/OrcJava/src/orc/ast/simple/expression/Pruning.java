package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.oil.expression.Expr;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;

public class Pruning extends Expression {

	Expression left;
	Expression right;
	Var v;
	
	public Pruning(Expression left, Expression right, Var v)
	{
		this.left = left;
		this.right = right;
		this.v = v;
	}
	
	@Override
	public Expression subst(Argument a, NamedVar x) 
	{
		return new Pruning(left.subst(a,x), right.subst(a,x), v);
	}

	public Set<Var> vars() {
		
		Set<Var> s = left.vars();
		s.addAll(right.vars());
		s.remove(v);
		return s;
	}

	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		
		Env<Var> newvars = vars.clone();
		newvars.add(v);
		
		return new orc.ast.oil.expression.Pruning(left.convert(newvars, typevars), right.convert(vars, typevars), v.name);
	}
	
}
