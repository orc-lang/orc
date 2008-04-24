package orc.ast.simple.arg;

import orc.runtime.values.Value;


/**
 * Bound variables. Equivalence on these variables is physical (==) equality.
 * 
 * These occur in argument position. They also occur as fields in combinators
 * which bind variables.
 * 
 * @author dkitchin
 *
 */

public class Var extends Argument 
{

	public Value asValue()
	{
		throw new Error("Bound variable " + this.toString() + "can not be used as a value.");
	}

}
