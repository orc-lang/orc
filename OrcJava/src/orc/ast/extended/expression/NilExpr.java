package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.Site;
import orc.ast.simple.expression.WithLocation;

public class NilExpr extends Expression {
	
	public orc.ast.simple.expression.Expression simplify() {
		return new orc.ast.simple.expression.Call(new Site(orc.ast.sites.Site.NIL));
	}

	public String toString() {
		return "[]";
	} 

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
