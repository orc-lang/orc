package orc.lib.time;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.sites.Site;
import orc.runtime.values.Closure;

public class PopLtimer extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		caller.popLtimer();
		caller.resume();
	}
}
