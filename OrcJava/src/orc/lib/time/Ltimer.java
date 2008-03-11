package orc.lib.time;

import orc.runtime.Args;
import orc.runtime.OrcRuntimeTypeError;
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
			long delay = args.longArg(0);
			
			// Add this token to its home engine's logical time event list 
			caller.getEngine().getClock().addEvent(delay, caller);
			
		} catch (OrcRuntimeTypeError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
