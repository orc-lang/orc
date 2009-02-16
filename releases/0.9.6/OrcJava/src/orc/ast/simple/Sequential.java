package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.Expr;
import orc.ast.oil.Push;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
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


	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		
		Env<Var> newvars = vars.clone();
		newvars.add(v);
		
		return new Push(left.convert(vars, typevars), right.convert(newvars, typevars));
	}

}
