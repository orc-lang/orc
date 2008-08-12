package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.Let;
import orc.ast.simple.Parallel;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.PatternException;

public class PublishPattern extends Pattern {

	Pattern p;
	
	public PublishPattern(Pattern p) {
		this.p = p;
	}

//	public Expression bind(Var u, Expression g) {
//		return new Parallel(p.bind(u, g), new Let(u));
//	}
//
//	public Expression match(Var u) {
//		return p.match(u);
//	}

	public boolean strict() {
		return p.strict();
	}

	@Override
	public void process(Var fragment, PatternVisitor visitor) throws PatternException {
		p.process(fragment, visitor);
		visitor.publish(fragment);
	}
	
	
	public String toString() {
		return "!" + p;
	}
}
