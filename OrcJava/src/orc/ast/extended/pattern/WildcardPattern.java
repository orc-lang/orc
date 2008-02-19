package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;

public class WildcardPattern implements Pattern {
	
	public WildcardPattern() {}
	
	public Expression bind(Expression g, Var t) {
		return g;
	}

	public Expression match(Expression f) {
		return f;
	}

	public boolean strict() {
		return false;
	}

	
}
