package orc.runtime.values;

import java.io.Serializable;

import orc.runtime.RemoteToken;
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
 * If it is not ready, it throws an exception. As an additional effect,
 * the provided token is added to some wait set and will be activated
 * when this object makes progress towards being bound.
 * 
 * @author dkitchin
 */
public interface Future extends Serializable {
	public Callable forceCall(Token t) throws FutureUnboundException;
	public Value forceArg(Token t) throws FutureUnboundException;
}