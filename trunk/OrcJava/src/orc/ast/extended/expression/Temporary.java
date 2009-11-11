package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;

/**
 * 
 * A temporary variable, which embeds a Variable object from the simple
 * AST within the extended AST. Used to avoid the problem of generating
 * string identifiers for unnamed vars in translation steps.
 *
 * @author dkitchin
 */
public class Temporary extends Expression {

	public Variable v;
	
	public Temporary(Variable v) {
		this.v = v;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() {
		return new orc.ast.simple.expression.Let(v);
	}

	public Arg argify() {
		return new simpleArg(v);
	}
	
	public String toString() {
		return "_temp" + v.hashCode();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
