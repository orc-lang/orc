package orc.runtime.values;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Set;

import orc.runtime.RemoteToken;
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
	Set<Future> freeset;
	
	public PartialValue(Value v, Set<Future> freeset)
	{
		this.v = v;
		this.freeset = freeset;
	}

	public Value forceArg(Token t) {
		
		// If there are unbound variables, try to force each one.
		// Release the value only if all of the variables have been bound.
		if (freeset != null) {
			try {
				for (Future f : freeset) f.forceArg(t);
				freeset = null;
			} catch (FutureUnboundException e) {
				return null;
			}
		}
		return v.forceArg(t);
	}

	public Callable forceCall(Token t) {
		
		// Don't need to force unbound variables for a value used in call position,
		// since it can't leak variables in that case. Just force the underlying value.
		return v.forceCall(t);
	}
}