package orc.runtime.values;

import orc.error.runtime.UncallableValueException;
import orc.runtime.Token;

/**
 * Interface for values which may be unbound (i.e. those generated by asymmetric
 * composition). Any class implementing Future can be forced to try and obtain
 * its bound value.
 * 
 * @see Value#forceArg(Object, Token)
 * @see Value#forceCall(Object, Token)
 * @author dkitchin
 */

public interface Future {
	public Callable forceCall(Token t) throws UncallableValueException;
	public Object forceArg(Token t);
}
