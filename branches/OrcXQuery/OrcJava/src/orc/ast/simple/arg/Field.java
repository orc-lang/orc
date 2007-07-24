package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Field access argument. Embeds a String key.
 * 
 * @author dkitchin
 */

public class Field extends Argument {
	
	public String key;
	
	public Field(String key)
	{
		this.key = key;
	}
	
	public Value asValue()
	{
		return new orc.runtime.values.Constant(key);
	}
	
}
