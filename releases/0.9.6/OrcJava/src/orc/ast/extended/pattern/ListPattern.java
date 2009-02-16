package orc.ast.extended.pattern;

import java.util.Iterator;
import java.util.List;

import orc.ast.simple.Expression;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;

public class ListPattern extends Pattern {

	List<Pattern> ps;
	
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
	public void process(Var fragment, PatternSimplifier visitor)
			throws PatternException {
		// HACK: a list pattern is precisely equivalent to a series of cons
		// patterns terminated by a nil pattern. However we want to record
		// source location information slightly differently, so we have to
		// inline and slightly change the equivalent Cons/NilPatterns.
		boolean hasLocation = false;
		
		for (Pattern p : ps) {
			Var pair = new Var();
			Expression e = Pattern.trycons(fragment);
			if (!hasLocation) {
				e = new WithLocation(e, getSourceLocation());
				hasLocation = true;
			}
			visitor.assign(pair, e);
			visitor.require(pair);
			
			Var head = new Var();
			visitor.assign(head, Pattern.nth(pair, 0));
			p.process(head, visitor);
			
			fragment = new Var();
			visitor.assign(fragment, Pattern.nth(pair, 1));
		}
		
		Var nilp = new Var();
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
}
