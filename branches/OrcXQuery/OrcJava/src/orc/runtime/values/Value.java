package orc.runtime.values;

import orc.runtime.Token;

/**
 * 
 * A fully evaluated Orc value. This includes sites, tuples of fully evaluated values, 
 * and native Java values.
 * 
 * However, it does not include unbound or partially-bound values, which are instead in 
 * the broader category of Futures.
 * 
 * @author dkitchin
 *
 */
public class Value implements Future {

	// A value is already computed, so it forces to itself.
	public Value forceArg(Token t)
	{
		return this;
	}
	
	// If this value is callable, return it, otherwise return an error.
	// This is the default behavior; subclasses may override it.
	public Callable forceCall(Token t)
	{
		if (this instanceof Callable)
		{
			return (Callable)this;
		}
		else
		{
			throw new Error(this.toString() + " is not a callable value.");
		}
			
	}
	
	/**
	 * Static function to access the canonical 'signal' value
	 * 
	 * Currently, the signal value is an empty tuple
	 */
	public static Value signal() {
		return new Tuple();
	}
	
}
