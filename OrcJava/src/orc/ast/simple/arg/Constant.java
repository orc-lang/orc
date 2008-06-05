package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Program constants, which occur in argument position. 
 * 
 * @author dkitchin
 *
 */

public class Constant extends Argument {
	private static final long serialVersionUID = 1L;
	public Object val;
	
	public Constant(Object val)
	{
		this.val = val;
	}
	
	public Value asValue()
	{
		return new orc.runtime.values.Constant(val);
	}
	public String toString() {
		return super.toString() + "(" + val + ")";
	}
}
