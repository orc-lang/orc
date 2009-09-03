package orc.ast.extended.pattern;

import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.Field;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.Parallel;
import orc.ast.simple.expression.Sequential;
import orc.ast.simple.expression.Pruning;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class CallPattern extends Pattern {

	public FreeVariable site;
	public Pattern p;
	
	// Create a call based on a string name
	public CallPattern(String site, List<Pattern> args) {
		this.site = new FreeVariable(site);
		this.p = Pattern.condense(args);
	}
	
	@Override
	public void process(Variable fragment, PatternSimplifier visitor)
			throws PatternException {
		
		Variable result = new Variable();
		visitor.assign(result, new WithLocation(
				Pattern.unapply(site, fragment),
				getSourceLocation()));
		visitor.require(result);
		p.process(result, visitor);
	}
	
	public String toString() {
		return site.name + p.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
