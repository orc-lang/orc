package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

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
	
	public Type type() {
		return new ArrowType(Type.NUMBER);
	}

}
