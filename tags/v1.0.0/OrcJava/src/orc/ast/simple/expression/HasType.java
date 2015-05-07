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


/**
 * 
 * An expression with an ascribed syntactic type.
 * 
 * @author dkitchin
 *
 */
public class HasType extends Expression {

	public Expression body;
	public Type type;
	public boolean checkable; // set false if this is a type assertion, not a type ascription
	
	public HasType(Expression body, Type type, boolean checkable) {
		this.body = body;
		this.type = type;
		this.checkable = checkable;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.HasType(body.convert(vars,typevars), type.convert(typevars), checkable);
	}

	@Override
	public Expression subst(Argument a, FreeVariable x) {
		return new HasType(body.subst(a,x), type, checkable);
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return new HasType(body.subst(T,X), type.subst(T, X), checkable);
	}

	@Override
	public Set<Variable> vars() {
		return body.vars();
	}

}
