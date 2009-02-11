package orc.runtime.sites.core;

import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.ArrowType;
import orc.type.Type;

public class Error extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		caller.error(new SiteException(args.stringArg(0)));
	}
	
	public Type type() {
		return new ArrowType(Type.STRING, Type.BOT);
	}
}
