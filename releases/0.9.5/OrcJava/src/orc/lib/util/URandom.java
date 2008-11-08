package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;

public class URandom extends PartialSite {

	java.util.Random rnd;
	
	public URandom() {
		rnd = new java.util.Random();
	}
	
	@Override
	public Object evaluate(Args args) throws TokenException {
		if (args.size() == 0) {
			return rnd.nextDouble();
		}
		else {
			return null;
		}
	}

}
