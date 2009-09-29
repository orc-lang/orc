package orc.ast.extended.pattern;

import java.util.Iterator;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Expression;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.PatternException;

public class ListPattern extends Pattern {

	public List<Pattern> ps;
	
	public ListPattern(List<Pattern> ps) {
		this.ps = ps;
	}
	
	
//	public Expression bind(Var u, Expression g) {
//		
//		return actual.bind(u,g);
//	}
//
//	public Expression match(Var u) {
//		return actual.match(u);
//	}


	@Override
	public void process(Variable fragment, PatternSimplifier visitor)
			throws PatternException {
		// HACK: a list pattern is precisely equivalent to a series of cons
		// patterns terminated by a nil pattern. However we want to record
		// source location information slightly differently, so we have to
		// inline and slightly change the equivalent Cons/NilPatterns.
		boolean hasLocation = false;
		
		for (Pattern p : ps) {
			Variable pair = new Variable();
			Expression e = Pattern.trycons(fragment);
			if (!hasLocation) {
				e = new WithLocation(e, getSourceLocation());
				hasLocation = true;
			}
			visitor.assign(pair, e);
			visitor.require(pair);
			
			Variable head = new Variable();
			visitor.assign(head, Pattern.nth(pair, 0));
			p.process(head, visitor);
			
			fragment = new Variable();
			visitor.assign(fragment, Pattern.nth(pair, 1));
		}
		
		Variable nilp = new Variable();
		Expression e = Pattern.trynil(fragment);
		if (!hasLocation) {
			e = new WithLocation(e, getSourceLocation());
		}
		visitor.assign(nilp, e);
		visitor.require(nilp);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		Iterator<Pattern> psi = ps.iterator();
		if (psi.hasNext()) {
			sb.append(psi.next().toString());
			while (psi.hasNext()) {
				sb.append(psi.next().toString());
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
