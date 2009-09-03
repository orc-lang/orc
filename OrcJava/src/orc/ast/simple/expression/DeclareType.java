package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A syntactic type declaration. 
 * 
 * @author dkitchin
 *
 */

public class DeclareType extends Expression {
	
	public Type type;
	public TypeVariable name;
	public Expression body;
	
	public DeclareType(Type type, TypeVariable name, Expression body) {
		this.type = type;
		this.name = name;
		this.body = body;
	}
	
	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars)
			throws CompilationException {
	
		orc.ast.oil.type.Type newtype = type.convert(typevars);
		orc.ast.oil.expression.Expression newbody = body.convert(vars, typevars.extend(name));
		
		return new orc.ast.oil.expression.DeclareType(newtype, newbody);
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x) {
		return new DeclareType(type, name, body.subst(a,x));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new DeclareType(type.subst(T,X), name, body.subst(T,X));
	}
	
	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

	public String toString() {
		return "(\n" + "type " + name.name + " = " + type + "\n" + body + "\n)";  
	}
	
}
