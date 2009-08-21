package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A syntactic type declaration. Since this declaration binds a type variable,
 * the variable name is still a string.
 * 
 * @author dkitchin
 *
 */

public class DeclareType extends Expression {
	
	public Type type;
	public String name;
	public Expression body;
	
	public DeclareType(Type type, String name, Expression body) {
		this.type = type;
		this.name = name;
		this.body = body;
	}
	
	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars)
			throws CompilationException {
		
		orc.type.Type newtype = type.convert(typevars);
		orc.ast.oil.expression.Expression newbody = body.convert(vars, typevars.extend(name));
		
		return new orc.ast.oil.expression.DeclareType(newtype, newbody);
	}
	
	@Override
	public Expression subst(Argument a, NamedVariable x) {
		return new DeclareType(type, name, body.subst(a,x));
	}
	
	@Override
	public Set<Variable> vars() {
		return body.vars();
	}
	
	
	
	
	
}
