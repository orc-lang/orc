package orc.lib.time;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.LogicalClock;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * 
 * Site interface to the Orc engine's logical clock.
 * 
 * @author dkitchin
 *
 */


public class MakeTimer extends Site {
	
	public void callSite(Args args, Token caller) {
		
		LogicalClock clock = new LogicalClock();
		caller.getEngine().addClock(clock);
		caller.resume(new LTimer(clock));
	}
	
	class LTimer extends Site {

		LogicalClock clock;
		
		public LTimer(LogicalClock clock) {
			this.clock = clock;
		}
		
		@Override
		public void callSite(Args args, Token caller) throws TokenException {
			
			int delay = args.intArg(0);
				
			// Add the caller to this timer's logical time event list 
			clock.addEvent(delay, caller);
		}

	}
	
}



