package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.Let;
import orc.ast.simple.Parallel;
import orc.ast.simple.arg.Var;

public class PublishPattern implements Pattern {

	Pattern p;
	
	public PublishPattern(Pattern p) {
		this.p = p;
	}

	public Expression bind(Expression g, Var t) {
		return new Parallel(p.bind(g, t), new Let(t));
	}

	public Expression match(Expression f) {
		return p.match(f);
	}

	public boolean strict() {
		return p.strict();
	}
}
