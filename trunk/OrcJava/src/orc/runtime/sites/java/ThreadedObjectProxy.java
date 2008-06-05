package orc.runtime.sites.java;

import orc.error.OrcRuntimeTypeException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.values.Value;

public class ThreadedObjectProxy extends ObjectProxy {
	public ThreadedObjectProxy(Object inst) {
		super(inst);
	}
	public Value evaluate(Args args) throws OrcRuntimeTypeException {
		final MethodProxy proxy = (MethodProxy)getMethodProxy(args);
		return new ThreadedSite() {
			public Value evaluate(Args args) throws OrcRuntimeTypeException {
				return proxy.evaluate(args);
			}
		};
	}
}
