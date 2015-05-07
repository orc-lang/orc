package orc.runtime.sites.core;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

public class Error extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		caller.error(new SiteException(args.stringArg(0)));
	}
}
