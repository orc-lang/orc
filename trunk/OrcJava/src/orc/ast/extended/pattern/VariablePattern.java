package orc.ast.extended.pattern;

import orc.ast.simple.Expression;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;

public class VariablePattern implements Pattern {

	NamedVar x;
	
	public VariablePattern(String s)
	{
		x = new NamedVar(s);
	}
	
	public Expression bind(Expression g, Var t) {
		return g.subst(t, x);
	}

	public Expression match(Expression f) {
		return f;
	}

	public boolean strict() {
		return false;
	}
	
}
