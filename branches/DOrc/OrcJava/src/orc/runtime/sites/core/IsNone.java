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
public class IsNone extends Site implements PassedByValueSite {
	@Override
	public void callSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException {
		
		Value v = args.valArg(0);
		
		if (v.isNone()) {
			try {
				caller.resume(Value.signal());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
		else {
			try {
				caller.die();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
	}
}
