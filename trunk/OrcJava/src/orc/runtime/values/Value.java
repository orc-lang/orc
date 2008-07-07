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
public abstract class Value implements Future {

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
		return new TupleValue();
	}
	
	
	/**
	 * Test whether this value inhabits one of the option or list types. 
	 */
	public boolean isSome() { return false; }
	public boolean isNone() { return false; }
	public boolean isCons() { return false; }
	public boolean isNil() { return false; }
	
	/**
	 * Return the contained value v of an option some(v).
	 * This method must throw an exception iff isSome() returns false.
	 */
	public Value untag() { throw new Error(); }
	
	/**
	 * Return the head value of a cons-like data structure.
	 * This method must throw an exception iff isCons() returns false.
	 */
	public Value head() { throw new Error(); }
	
	/**
	 * Return the tail value of a cons-like data structure.
	 * This method must throw an exception iff isCons() returns false.
	 */
	public Value tail() { throw new Error(); }

	public abstract <E> E accept(Visitor<E> visitor);
}
