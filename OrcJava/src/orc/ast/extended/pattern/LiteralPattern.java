package orc.ast.extended.pattern;

import orc.ast.extended.Literal;
import orc.ast.simple.*;
import orc.ast.simple.arg.*;
import xtc.util.Utilities;

public class LiteralPattern extends Pattern {

	Literal lit;
	
	public LiteralPattern(Literal l) {
		this.lit = l;
	}
	
	public Expression bind(Var u, Expression g) {
		return g;
	}

	public Expression match(Var u) {
		Argument arg = lit.argify().asArg();
		// u = L
		Expression test = new Call(Pattern.EQUAL, u, arg); 
		
		// some(L)
		Expression tc = new Call(Pattern.SOME, arg);
		
		// none()
		Expression fc = new Call(Pattern.NONE);
		
		// if (u=L) then some(L) else none()
		return Pattern.ifexp(test, tc, fc);
	}
	
	public String toString() {
		return lit.toString();
	}
}
