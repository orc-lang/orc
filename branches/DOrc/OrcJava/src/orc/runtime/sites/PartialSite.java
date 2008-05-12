/**
 * 
 */
package orc.runtime.sites;

import java.rmi.RemoteException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.values.Value;

/**
 * Abstract class for sites with a partial and immediate semantics: evaluate as for a total
 * immediate site (see EvalSite), but if the evaluation returns null, the site remains silent.
 * The site "if" is a good example.
 * 
 * Subclasses must implement the method evaluate, which takes an argument list and returns
 * a single value (possibly null).
 * 
 * Like EvalSite, this cannot in general be passed by value, although
 * in most cases it should be.
 *
 * @author dkitchin
 *
 */
public abstract class PartialSite extends Site {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public void callSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException, RemoteException {
		
		Value v = evaluate(args);
		if (v != null) {
			caller.resume(v);
		}
		else {
			caller.die();
		}	
		
	}
	
	abstract public Value evaluate(Args args) throws OrcRuntimeTypeException;

}
