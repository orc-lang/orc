package orc.ast.simple.argument;


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
		return "." + key;
	}

	@Override
	public orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) {
		return new orc.ast.oil.expression.argument.Field(key);
	}
}