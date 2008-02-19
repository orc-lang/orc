package orc.ast.extended.pattern;

import java.util.LinkedList;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;

public class NilPattern implements Pattern {

	Pattern actual;
	
	public NilPattern() { 
		actual = new CallPattern("nil", new LinkedList<Pattern>());
	}
	
	public Expression bind(Expression g, Var t) {
		return actual.bind(g,t);
	}

	public Expression match(Expression f) {
		return actual.match(f);
	}

	public boolean strict() {
		return true;
	}
	
}
