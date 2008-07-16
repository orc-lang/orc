package orc.runtime.sites.java;

import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.Value;

public class ThreadedObjectProxy extends ObjectProxy {
	public ThreadedObjectProxy(Object inst) {
		super(inst);
	}
	public Value evaluate(Args args) throws TokenException {
		final MethodProxy proxy = (MethodProxy)getMethodProxy(args.fieldName());
		return new ThreadedSite() {
			public Value evaluate(Args args) throws TokenException {
				return proxy.evaluate(args);
			}
		};
	}
}
