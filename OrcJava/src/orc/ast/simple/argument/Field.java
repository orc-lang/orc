package orc.ast.simple.argument;


import orc.ast.oil.expression.argument.Arg;
import orc.env.Env;
import orc.runtime.values.Value;


/**
 * Field access argument. Embeds a String key.
 * 
 * @author dkitchin
 */

public class Field extends Argument {
	private static final long serialVersionUID = 1L;
	public String key;
	
	public Field(String key)
	{
		this.key = key;
	}
	
	public String toString() {
		return super.toString() + "(" + key + ")";
	}

	@Override
	public Arg convert(Env<Var> vars) {
		
		return new orc.ast.oil.expression.argument.Field(key);
	}
}