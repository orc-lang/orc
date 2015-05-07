package orc.ast.extended;

import java.util.Arrays;

import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Var;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.Sequential;
import orc.ast.simple.Where;
import orc.ast.simple.Parallel;
import orc.ast.simple.arg.Site;
import orc.ast.simple.Call;
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
		this(condition, consequent, new Silent());
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		// store the result of the condition
		Var c = new Var();
		
		// thenExpr = if(c) >> consequent
		orc.ast.simple.Expression thenExpr = new Sequential(
			new Call(
				new Site(orc.ast.sites.Site.IF),
				Arrays.asList(new Argument[]{c})),
			consequent.simplify(),
			new Var());
		
		orc.ast.simple.Expression body;
		if (alternative instanceof Silent) {
			// if the alternative is silent, we can omit it
			body = thenExpr;
		} else {
			// elseExpr = not(c) >x> (if(x) >> alternative)
			Var x = new Var();
			orc.ast.simple.Expression elseExpr = new Sequential(
				new Call(
					new Site(orc.ast.sites.Site.NOT),
					Arrays.asList(new Argument[]{c})),
				new Sequential(
					new Call(
						new Site(orc.ast.sites.Site.IF),
						Arrays.asList(new Argument[]{x})),
					alternative.simplify(),
					new Var()),
				x);
			body = new Parallel(thenExpr, elseExpr);
		}
		
		// body <c< condition
		return new WithLocation(
			new Where(body, condition.simplify(), c),
			getSourceLocation());
	}
	
	public String toString() {
		return "(if " + condition + " then " + consequent + " else " + alternative + ")";
	}
}
