package orc.ast.simple.argument;


import orc.env.Env;
import orc.error.compiletime.UnboundVariableException;
import orc.runtime.values.Value;


/**
 * Free variables. All such variables embed a String key.
 * Equivalence on these variables is equality of the embedded string.
 * 
 * Like normal Variables, these occur in argument position. 
 * 
 * The subst method on simplified expressions can only substitute for
 * a free variable.
 * 
 * @author dkitchin
 */

public class FreeVariable extends Argument implements Comparable<FreeVariable> {
	private static final long serialVersionUID = 1L;
	public String name;
	
	public FreeVariable(String key)
	{
		this.name = key;
	}
	
	public int compareTo(FreeVariable f) {
		String s = this.name;
		String t = f.name;
		return s.compareTo(t);
	}
	
	public boolean equals(Object o) {
		
		if (o instanceof FreeVariable)
		{
			return (this.compareTo((FreeVariable)o) == 0);
		}
		else
		{
			return this.equals(o);
		}
	}
	public String toString() {
		return name;
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) throws UnboundVariableException {
		throw new UnboundVariableException(name, getSourceLocation());
	}
}
