package orc.ast.extended.pattern;

import orc.ast.extended.Literal;
import orc.ast.simple.*;
import orc.ast.simple.arg.*;

public class LiteralPattern extends Pattern {

	Argument lit;
	
	public LiteralPattern(Literal l) {
		this.lit = l.argify().asArg();
	}
	
	public Expression bind(Var u, Expression g) {
		return g;
	}

	public Expression match(Var u) {
		// u = L
		Expression test = new Call(Pattern.EQUAL, u, lit); 
		
		// some(L)
		Expression tc = new Call(Pattern.SOME, lit);
		
		// none()
		Expression fc = new Call(Pattern.NONE);
		
		// if (u=L) then some(L) else none()
		return Pattern.ifexp(test, tc, fc);
	}
	
}
