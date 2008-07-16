package orc.runtime.values;

import orc.error.UncallableValueException;
import orc.runtime.Token;

/**
 * 
 * Common interface for both bound values and unbound cells.
 * Any class implementing Future can be forced, which will
 * have one of two results:
 * 
 * If the object is ready, it returns with the appropriate entity 
 * (a Callable or a Value).
 * 
 * If it is not ready, it returns null. As an additional effect,
 * the provided token is added to some wait set and will be activated
 * when this object makes progress towards being bound.
 * 
 * @author dkitchin
 *
 */

public interface Future {
	
	public Callable forceCall(Token t) throws UncallableValueException;
	public Value forceArg(Token t);

}
