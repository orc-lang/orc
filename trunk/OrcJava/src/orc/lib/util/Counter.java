package orc.lib.util;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.PartialSite;

/**
 * Factory for counters. 
 * A counter publishes the first n times it is called,
 * where n is set when it is created.
 * @author quark
 */
public class Counter extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		final int limit = args.intArg(0);
		return new PartialSite() {
			private int count = 0;
			@Override
			public Object evaluate(Args args) throws TokenException {
				if (++count <= limit) return signal();
				else return null;
			}
		};
	}
}
