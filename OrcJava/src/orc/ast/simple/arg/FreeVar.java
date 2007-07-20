package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Free variables. All free variables embed a String key.
 * Equivalence on these variables is equality of the embedded string.
 * 
 * Like normal Vars, these occur in argument position. However, since they
 * can never be bound at runtime, they compile to dead nodes.
 * 
 * The subst method of simplified expressions can only substitute for
 * a FreeVar.
 * 
 * @author dkitchin
 */

public class FreeVar extends Argument {
	
	String key;
	
	public FreeVar(String key)
	{
		this.key = key;
	}

	public boolean equals(Object o)
	{
		if (o instanceof FreeVar)
		{
			FreeVar f = (FreeVar)o;
			return f.keycompare(this);
		}
		else
		{
			return o.equals(this);
		}
	}
	
	public boolean keycompare(FreeVar f)
	{
		return f.key.equals(this.key);
	}
	
	public Value asValue()
	{
		throw new Error("Free variable " + key + " can never be bound to a value.");
	}
}
