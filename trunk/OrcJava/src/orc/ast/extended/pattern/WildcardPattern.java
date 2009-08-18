package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;

public class WildcardPattern extends Pattern {
	public boolean strict() {
		return false;
	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor) {
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
