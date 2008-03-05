package orc.ast.extended.pattern;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.arg.Var;

public class NilPattern extends Pattern {
	
	public Expression bind(Var u, Expression g) {
		return g;
	}

	public Expression match(Var u) {
		
		return new Call(Pattern.ISNIL, u);
	}

}
