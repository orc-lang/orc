package orc.ast.simple.arg;

import orc.runtime.values.Value;

/**
 * 
 * An AST class (distinct from Expression) which contains arguments to calls.
 * These arguments may either be variable names or constant values.
 * 
 * Note that in the call M(x,y), M is also technically an argument of the call.
 * This allows variables to be used in call position.
 * 
 * @author dkitchin
 *
 */

public abstract class Argument {
	
	public abstract Value asValue();
}
