package orc.runtime.sites.java;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.PartialSite;
import orc.runtime.sites.Site;

public class MatchProxy extends PartialSite {

	public Class cls;
	
	public MatchProxy(Class cls) {
		this.cls = cls;
	}

	public Object evaluate(Args args) throws TokenException {

		/* Note: A match proxy will not match null, regardless of the class to be matched.
		 * Currently this is due to its implementation as a PartialSite, but more generally,
		 * since it is not possible to call null.getClass(), considering null to be of any
		 * particular class seems meaningless.
		 */
		
		Object arg = args.getArg(0);
		
		if (cls.isAssignableFrom(arg.getClass())) {
			return arg;
		}
		else {
			return null;
		}
	}

}
