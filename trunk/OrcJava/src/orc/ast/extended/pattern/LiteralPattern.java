package orc.ast.extended.pattern;

import orc.ast.extended.Literal;
import orc.ast.simple.*;
import orc.ast.simple.arg.*;

public class LiteralPattern implements Pattern {

	Argument lit;
	
	public LiteralPattern(Literal l) {
		this.lit = l.argify().asArg();
	}
	
	public Expression bind(Expression g, Var t) {
		
		
		
		return null;
	}

	public Expression match(Expression f) {
		
		// t
	    Var fResult = new Var();
		
	    // t = L
		Expression eqExpr = new Call(new NamedVar("op="), fResult, lit);
		
		// if(b) <b< (t = L)
		Var eqResult = new Var();
		Expression ifCall = new Call(new NamedVar("if"), eqResult);
		Expression ifExpr = new Where(ifCall, eqExpr, eqResult);
		
		// f >t> ( if(b) <b< (t = L) )
		Expression filter = new Sequential(f, ifExpr, fResult);
		
		// ( f >t> ( if(b) <b< (t = L) ) ) >> let(t)
		return new Sequential(filter, new Let(fResult), new Var());
	}
	
	public boolean strict() {
		return true;
	}

}
