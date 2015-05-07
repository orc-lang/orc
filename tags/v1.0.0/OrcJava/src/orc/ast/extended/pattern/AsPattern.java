package orc.ast.extended.pattern;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.error.compiletime.PatternException;

public class AsPattern extends Pattern {

	public Pattern p;
	public FreeVariable x;
	
	public AsPattern(Pattern p, String s) {
		this.p = p;
		this.x = new FreeVariable(s);
	}

//	public Expression bind(Var u, Expression g) {
//		
//		Expression h = g.subst(u, x);
//		return p.bind(u,h);
//	}
//
//	public Expression match(Var u) {
//		
//		return p.match(u);
//	}

	public boolean strict() {
		
		return p.strict();
	}

	@Override
	public void process(Variable fragment, PatternSimplifier visitor)
			throws PatternException {
		
		visitor.subst(fragment, x);
		p.process(fragment, visitor);
	}
	
	public String toString() {
		return "(" + p + " as " + x.name +")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
