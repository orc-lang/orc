package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.extended.expression.Literal;
import orc.ast.simple.*;
import orc.ast.simple.argument.*;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;
import xtc.util.Utilities;

public class LiteralPattern extends Pattern {

	public Literal lit;
	
	public LiteralPattern(Literal l) {
		this.lit = l;
	}
	
//	public Expression bind(Var u, Expression g) {
//		return g;
//	}
//
//	public Expression match(Var u) {
//		// u = L
//		Expression test = new Call(Pattern.EQUAL, u, lit); 
//		
//		// some(L)
//		Expression tc = new Call(Pattern.SOME, lit);
//		
//		// none()
//		Expression fc = new Call(Pattern.NONE);
//		
//		// if (u=L) then some(L) else none()
//		return Pattern.ifexp(test, tc, fc);
//	}

	@Override
	public void process(Variable fragment, PatternSimplifier visitor)
			throws PatternException {
		Variable test = new Variable();
		visitor.assign(test, new WithLocation(
				Pattern.compare(fragment, lit.argify().asArg()),
				getSourceLocation()));
		visitor.require(test);
	}
	
	public String toString() {
		return String.valueOf(lit);
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
