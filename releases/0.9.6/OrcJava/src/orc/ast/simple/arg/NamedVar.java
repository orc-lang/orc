package orc.ast.simple.arg;


import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.UnboundVariableException;
import orc.runtime.values.Value;


/**
 * Named (implicitly, free) variables. All such variables embed a String key.
 * Equivalence on these variables is equality of the embedded string.
 * 
 * Like normal Vars, these occur in argument position. However, since they
 * can never be bound at runtime, they compile to dead nodes.
 * 
 * The subst method on simplified expressions can only substitute for
 * a named variable.
 * 
 * @author dkitchin
 */

public class NamedVar extends Argument implements Comparable<NamedVar> {
	private static final long serialVersionUID = 1L;
	public String key;
	
	public NamedVar(String key)
	{
		this.key = key;
	}
	
	public int compareTo(NamedVar f) {
		String s = this.key;
		String t = f.key;
		return s.compareTo(t);
	}
	
	public boolean equals(Object o) {
		
		if (o instanceof NamedVar)
		{
			return (this.compareTo((NamedVar)o) == 0);
		}
		else
		{
			return this.equals(o);
		}
	}
	public String toString() {
		return super.toString() + "(" + key + ")";
	}

	@Override
	public Arg convert(Env<Var> vars) throws UnboundVariableException {
		throw new UnboundVariableException(key, getSourceLocation());
	}
}
