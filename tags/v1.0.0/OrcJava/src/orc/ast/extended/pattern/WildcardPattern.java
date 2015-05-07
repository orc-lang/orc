package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Expression;

public class WildcardPattern extends Pattern {
	public boolean strict() {
		return false;
	}

	@Override
	public void process(Variable fragment, PatternSimplifier visitor) {
		// Do nothing.
	}

	
	public String toString() {
		return "_";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
