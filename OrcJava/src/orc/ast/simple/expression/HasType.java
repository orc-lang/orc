package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.extended.type.Type;
import orc.ast.oil.expression.Expr;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
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
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.expression.HasType(body.convert(vars,typevars), type.convert(typevars), checkable);
	}

	@Override
	public Expression subst(Argument a, NamedVar x) {
		return new HasType(body.subst(a,x), type, checkable);
	}

	@Override
	public Set<Var> vars() {
		return body.vars();
	}

}
