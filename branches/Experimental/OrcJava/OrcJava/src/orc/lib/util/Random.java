package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

public class Random extends PartialSite {

	java.util.Random rnd;
	
	public Random() {
		rnd = new java.util.Random();
	}
	
	@Override
	public Object evaluate(Args args) throws TokenException {
		if (args.size() == 0) {
			return rnd.nextInt();
		}
		
		int limit = args.intArg(0);
		
		if (limit > 0) {
			return rnd.nextInt(limit);
		} else {
			return null;
		}
	}

	public Type type() {
		return new MultiType(
				new ArrowType(Type.INTEGER),
				new ArrowType(Type.INTEGER, Type.INTEGER));
	}

}
