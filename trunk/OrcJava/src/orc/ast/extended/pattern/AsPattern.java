package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

public class AsPattern implements Pattern {

	Pattern p;
	NamedVar x;
	
	public AsPattern(Pattern p, String s) {
		this.p = p;
		this.x = new NamedVar(s);
	}

	public Expression bind(Expression g, Var t) {
		
		Expression h = g.subst(t, x);
		return p.bind(h,t);
	}

	public Expression match(Expression f) {
		
		return p.match(f);
	}

	public boolean strict() {
		
		return p.strict();
	}
	
}
