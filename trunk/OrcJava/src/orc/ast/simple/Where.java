package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.Expr;
import orc.ast.oil.Pull;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
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
	public Expression subst(Argument a, NamedVar x) 
	{
		return new Where(left.subst(a,x), right.subst(a,x), v);
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
		
		return new Pull(left.convert(newvars, typevars), right.convert(vars, typevars), v.name);
	}
	
}
