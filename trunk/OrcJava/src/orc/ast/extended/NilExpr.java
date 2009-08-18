package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Site;

public class NilExpr extends Expression {
	
	public orc.ast.simple.Expression simplify() {
		return new orc.ast.simple.Call(new Site(orc.ast.sites.Site.NIL));
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
