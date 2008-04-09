package orc.lib.time;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * 
 * Site interface to the Orc engine's logical clock.
 * 
 * @author dkitchin
 *
 */

public class Ltimer extends Site {

	@Override
	public void callSite(Args args, Token caller) {
		
		try {
			int delay = args.intArg(0);
			
			// Add this token to its home engine's logical time event list 
			caller.getEngine().getClock().addEvent(delay, caller);
			
		} catch (OrcRuntimeTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
