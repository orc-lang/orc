package orc.ast.extended.pattern;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;

public class ConsPattern implements Pattern {

	Pattern actual;
	
	public ConsPattern(Pattern h, Pattern t) {
		
		List<Pattern> args = new LinkedList<Pattern>();
		args.add(h);
		args.add(t);
		actual = new CallPattern("cons", args);
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
