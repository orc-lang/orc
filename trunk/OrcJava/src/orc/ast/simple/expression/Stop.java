package orc.ast.simple.expression;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.runtime.nodes.Node;

public class Stop extends Expression {


	@Override
	public Expression subst(Argument a, FreeVariable x) {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return this;
	}

	@Override
	public Set<Variable> vars() {
		return new HashSet<Variable>();
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) {
		return new orc.ast.oil.expression.Stop();
	}
	
	public String toString() {
		return "stop";
	}
	
}
