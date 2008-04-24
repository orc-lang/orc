package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Constant extends Argument {

	public Object val;
	
	public Constant(Object val)
	{
		this.val = val;
	}
	
	public Value asValue()
	{
		return new orc.runtime.values.Constant(val);
	}
	
}
