package orc.ast.extended.pattern;

import java.util.List;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;

public class ListPattern extends Pattern {

	Pattern actual;
	
	public ListPattern(List<Pattern> ps) {
		
		Pattern p = new NilPattern();
		
		for(int i = ps.size() - 1; i >= 0; i--) {
			p = new ConsPattern(ps.get(i), p);
		}
		
		actual = p; 
	}
	
	
//	public Expression bind(Var u, Expression g) {
//		
//		return actual.bind(u,g);
//	}
//
//	public Expression match(Var u) {
//		// TODO Auto-generated method stub
//		return actual.match(u);
//	}


	@Override
	public void process(Var fragment, PatternVisitor visitor)
			throws PatternException {
		
		actual.process(fragment, visitor);
	}
	
	
	
	public String toString() {
		return actual.toString();
	}
}
