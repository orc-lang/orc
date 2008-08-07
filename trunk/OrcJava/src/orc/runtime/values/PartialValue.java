package orc.runtime.values;

import java.util.Set;

import orc.error.runtime.UncallableValueException;
import orc.runtime.Token;

/**
 * 
 * A Value which depends on some set of potentially unbound variables, which
 * therefore cannot be used in argument position until those variables become
 * bound (because it might leak them).
 * 
 * @author dkitchin
 *
 */

public class PartialValue implements Future {
	
	Value v;
	Set<Object> freeset;
	
	public PartialValue(Value v, Set<Object> freeset)
	{
		this.v = v;
		this.freeset = freeset;
	}

	public Object forceArg(Token t) {
		
		// If there are unbound variables, try to force each one.
		// Release the value only if all of the variables have been bound.
		if (freeset != null) {
			for (Object f : freeset) {
				if (Value.forceArg(f, t) == Value.futureNotReady) {
					return Value.futureNotReady;
				}
			}
			freeset = null;
		}
		return Value.forceArg(v, t);
	}

	public Callable forceCall(Token t) throws UncallableValueException {
		// Don't need to force unbound variables for a value used in call position,
		// since it can't leak variables in that case. Just force the underlying value.
		return Value.forceCall(v, t);
	}
}
