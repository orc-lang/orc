package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.Expr;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Catch extends Expression {
	
	Definition handlerDef;
	Expression tryBlock;
	
	public Catch(Definition handlerDef, Expression tryBlock) {
		this.handlerDef = handlerDef;
		this.tryBlock = tryBlock;
	}
	
	//Performs the substitution [a/x]
	public Expression subst(Argument a, NamedVar x) 
	{
		return new Catch(handlerDef.subst(a, x), tryBlock.subst(a, x));
	}
	
	//Find the set of all unbound Vars (note: not FreeVars) in this expression.
	public Set<Var> vars(){
		Set<Var> s = handlerDef.vars();
		s.addAll(tryBlock.vars());
		return s;
	}
	
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException 
	{
		return new orc.ast.oil.Catch(handlerDef.convert(vars, typevars), tryBlock.convert(vars, typevars));
	}
}
