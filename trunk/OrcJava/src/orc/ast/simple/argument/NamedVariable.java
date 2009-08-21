package orc.ast.simple.argument;


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

public class NamedVariable extends Argument implements Comparable<NamedVariable> {
	private static final long serialVersionUID = 1L;
	public String name;
	
	public NamedVariable(String key)
	{
		this.name = key;
	}
	
	public int compareTo(NamedVariable f) {
		String s = this.name;
		String t = f.name;
		return s.compareTo(t);
	}
	
	public boolean equals(Object o) {
		
		if (o instanceof NamedVariable)
		{
			return (this.compareTo((NamedVariable)o) == 0);
		}
		else
		{
			return this.equals(o);
		}
	}
	public String toString() {
		return super.toString() + "(" + name + ")";
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) throws UnboundVariableException {
		throw new UnboundVariableException(name, getSourceLocation());
	}
}
