/**
 * 
 */
package orc.runtime.sites.core;

import java.rmi.RemoteException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.RemoteToken;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;

/**
 * @author dkitchin
 *
 */
public class IsSome extends Site implements PassedByValueSite {

	@Override
	public void callSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException, RemoteException {
		
		Value v = args.valArg(0);
		
		if (v.isSome()) {
			caller.resume(v.untag());
		}
		else {
			caller.die();
		}
	}

}
