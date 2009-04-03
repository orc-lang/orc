package orc.lib.time;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.type.ArrowType;
import orc.type.Type;

/**
 * Site interface to the Orc engine's logical clock.
 */
public class Ltimer extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		caller.delay(args.intArg(0));
	}

	public Type type() {
		return new ArrowType(Type.NUMBER, Type.TOP);
	}
	
}



