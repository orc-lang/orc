package orc.lib.time;

import java.rmi.RemoteException;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.LogicalClock;
import orc.runtime.RemoteToken;
import orc.runtime.sites.PassedByValueSite;
import orc.runtime.sites.Site;

/**
 * 
 * Site interface to the Orc engine's logical clock.
 * 
 * @author dkitchin
 *
 */
public class MakeTimer extends Site implements PassedByValueSite {
	public void callSite(Args args, RemoteToken caller) throws RemoteException {
		caller.resume(new orc.runtime.values.Site(new LTimer(caller.newClock())));
	}
	
	class LTimer extends Site implements PassedByValueSite {

		LogicalClock clock;
		
		public LTimer(LogicalClock clock) {
			this.clock = clock;
		}
		
		@Override
		public void callSite(Args args, RemoteToken caller) throws OrcRuntimeTypeException {
			
			int delay = args.intArg(0);
				
			// Add the caller to this timer's logical time event list 
			clock.addEvent(delay, caller);
		}
	}
}