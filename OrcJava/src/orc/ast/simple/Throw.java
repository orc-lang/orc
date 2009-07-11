package orc.ast.simple;

import java.util.Set;
import java.util.HashSet;

import orc.ast.oil.Expr;
import orc.ast.oil.arg.Arg;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Throw extends Expression {

	Expression exception;
	
	public Throw(Expression exception){
		this.exception = exception;
	}

	public Expression subst(Argument a, NamedVar x) 
	{
		return new Throw(exception.subst(a,x));
	}
	
	public Set<Var> vars(){
		Set s =  new HashSet<Var>();
		s.add(exception.vars());
		return s;
	}
	
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		orc.ast.oil.Expr e = exception.convert(vars, typevars);
		return new orc.ast.oil.Throw(e);
	}
}
