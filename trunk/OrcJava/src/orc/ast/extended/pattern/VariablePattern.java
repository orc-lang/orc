package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Call;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.NonlinearPatternException;

public class VariablePattern extends Pattern {

	public FreeVariable x;
	
	public VariablePattern(String s)
	{
		x = new FreeVariable(s);
	}
	
	public boolean strict() {
		return false;
	}

	@Override
	public void process(Variable fragment, PatternSimplifier visitor) throws NonlinearPatternException {
		visitor.subst(fragment, x);
	}
	
	public String toString() {
		return x.name.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
