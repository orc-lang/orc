package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
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
	public Expression subst(Argument a, FreeVariable x) 
	{
		return new Catch(handlerDef.subst(a, x), tryBlock.subst(a, x));
	}
	
	public Expression subst(Type T, FreeTypeVariable X) {
		return new Catch(handlerDef.subst(T, X), tryBlock.subst(T,X));
	}
	
	//Find the set of all unbound Vars (note: not FreeVars) in this expression.
	public Set<Variable> vars(){
		Set<Variable> s = handlerDef.vars();
		s.addAll(tryBlock.vars());
		return s;
	}
	
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException 
	{
		return new orc.ast.oil.expression.Catch(handlerDef.convert(vars, typevars), tryBlock.convert(vars, typevars));
	}
}
