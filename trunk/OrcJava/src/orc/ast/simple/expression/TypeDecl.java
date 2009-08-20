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
 * A syntactic type declaration. Since this declaration binds a type variable,
 * the variable name is still a string.
 * 
 * @author dkitchin
 *
 */

public class TypeDecl extends Expression {
	
	public Type type;
	public String name;
	public Expression body;
	
	public TypeDecl(Type type, String name, Expression body) {
		this.type = type;
		this.name = name;
		this.body = body;
	}
	
	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars)
			throws CompilationException {
		
		orc.type.Type newtype = type.convert(typevars);
		Expr newbody = body.convert(vars, typevars.extend(name));
		
		return new orc.ast.oil.expression.TypeDecl(newtype, newbody);
	}
	
	@Override
	public Expression subst(Argument a, NamedVar x) {
		return new TypeDecl(type, name, body.subst(a,x));
	}
	
	@Override
	public Set<Var> vars() {
		return body.vars();
	}
	
	
	
	
	
}
