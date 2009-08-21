package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.Site;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class ConsExpr extends Expression {

	public Expression h;
	public Expression t;
	
	public ConsExpr(Expression h, Expression t) {
		this.h = h;
		this.t = t;
	}

	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		Variable vh = new Variable();
		Variable vt = new Variable();
		
		orc.ast.simple.expression.Expression body = new orc.ast.simple.expression.Call(new Site(orc.ast.sites.Site.CONS), vh, vt);
		
		body = new orc.ast.simple.expression.Pruning(body, h.simplify(), vh);
		body = new orc.ast.simple.expression.Pruning(body, t.simplify(), vt);
		
		return new WithLocation(body, getSourceLocation());
	}
	
	public String toString() {
		return "(" + h + ":" + t + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
