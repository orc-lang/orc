package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public class Catch extends Expression {
	
	Def handlerDef;
	Expression tryBlock;
	
	public Catch(Def handlerDef, Expression tryBlock) {
		this.handlerDef = handlerDef;
		this.tryBlock = tryBlock;
	}
	
	//Performs the substitution [a/x]
	public Expression subst(Argument a, NamedVariable x) 
	{
		return new Catch(handlerDef.subst(a, x), tryBlock.subst(a, x));
	}
	
	//Find the set of all unbound Vars (note: not FreeVars) in this expression.
	public Set<Variable> vars(){
		Set<Variable> s = handlerDef.vars();
		s.addAll(tryBlock.vars());
		return s;
	}
	
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException 
	{
		return new orc.ast.oil.expression.Catch(handlerDef.convert(vars, typevars), tryBlock.convert(vars, typevars));
	}
}
