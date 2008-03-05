package orc.ast.extended.pattern;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

public class VariablePattern extends Pattern {

	NamedVar x;
	
	public VariablePattern(String s)
	{
		x = new NamedVar(s);
	}
	
	public Expression bind(Var u, Expression g) {
		return g.subst(u, x);
	}

	public Expression match(Var u) {
		return new Call(Pattern.SOME, u);
	}

	public boolean strict() {
		return false;
	}
	
}
