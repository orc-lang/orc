package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;

public class AsPattern extends Pattern {

	Pattern p;
	NamedVar x;
	
	public AsPattern(Pattern p, String s) {
		this.p = p;
		this.x = new NamedVar(s);
	}

//	public Expression bind(Var u, Expression g) {
//		
//		Expression h = g.subst(u, x);
//		return p.bind(u,h);
//	}
//
//	public Expression match(Var u) {
//		
//		return p.match(u);
//	}

	public boolean strict() {
		
		return p.strict();
	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor)
			throws PatternException {
		
		visitor.subst(fragment, x);
		p.process(fragment, visitor);
	}
	
	public String toString() {
		return "(" + p + " as " + x.key +")";
	}
}
