package orc.ast.extended.pattern;

import orc.ast.simple.Call;
import orc.ast.simple.Expression;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.NonlinearPatternException;

public class VariablePattern extends Pattern {

	NamedVar x;
	
	public VariablePattern(String s)
	{
		x = new NamedVar(s);
	}
	
	public boolean strict() {
		return false;
	}

	@Override
	public void process(Var fragment, PatternSimplifier visitor) throws NonlinearPatternException {
		visitor.subst(fragment, x);
	}
	
	public String toString() {
		return x.key.toString();
	}
}
