package orc.ast.extended.expression;

import java.util.Arrays;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.Site;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Parallel;
import orc.ast.simple.expression.Sequential;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * "if condition then consequent else alternative" desugars to
 * "(if(c) >> consequent | if(~c) >> alternative) &lt;c&lt; condition"
 * @author quark
 */
public class IfThenElse extends Expression {

	public Expression condition;
	public Expression consequent;
	public Expression alternative;
	
	public IfThenElse(Expression condition, Expression consequent, Expression alternative) {
		this.condition = condition;
		this.consequent = consequent;
		this.alternative = alternative;
	}
	
	public IfThenElse(Expression condition, Expression consequent) {
		this(condition, consequent, new Stop());
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		// store the result of the condition
		Variable c = new Variable();
		
		// thenExpr = if(c) >> consequent
		orc.ast.simple.expression.Expression thenExpr = new Sequential(
			new Call(
				new Site(orc.ast.sites.Site.IF),
				Arrays.asList(new Argument[]{c})),
			consequent.simplify(),
			new Variable());
		
		orc.ast.simple.expression.Expression body;
		if (alternative instanceof Stop) {
			// if the alternative is silent, we can omit it
			body = thenExpr;
		} else {
			// elseExpr = not(c) >x> (if(x) >> alternative)
			Variable x = new Variable();
			orc.ast.simple.expression.Expression elseExpr = new Sequential(
				new Call(
					new Site(orc.ast.sites.Site.NOT),
					Arrays.asList(new Argument[]{c})),
				new Sequential(
					new Call(
						new Site(orc.ast.sites.Site.IF),
						Arrays.asList(new Argument[]{x})),
					alternative.simplify(),
					new Variable()),
				x);
			body = new Parallel(thenExpr, elseExpr);
		}
		
		// body <c< condition
		return new WithLocation(
			new Pruning(body, condition.simplify(), c),
			getSourceLocation());
	}
	
	public String toString() {
		return "(if " + condition + " then " + consequent + " else " + alternative + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
