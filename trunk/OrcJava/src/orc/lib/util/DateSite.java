package orc.lib.util;

import java.util.Date;

import orc.error.runtime.InsufficientArgsException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

@SuppressWarnings("deprecation")
public class DateSite extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		switch (args.size()) {
		case 0: return new Date();
		case 1: return new Date(args.longArg(0));
		case 3: return new Date(args.intArg(0), args.intArg(1), args.intArg(2));
		case 5: return new Date(args.intArg(0), args.intArg(1), args.intArg(2),
					args.intArg(3), args.intArg(4));
		case 6: return new Date(args.intArg(0), args.intArg(1), args.intArg(2),
					args.intArg(3), args.intArg(4), args.intArg(5));
		default: throw new InsufficientArgsException(6, args.size());
		}
	}
}
