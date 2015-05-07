/**
 * 
 */
package orc.runtime.sites.java;


import java.lang.reflect.Array;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.lib.util.ThreadSite;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.Callable;

/**
 * A Java object being used as an Orc Site. This allows you to get a reference
 * to methods on the object using dot notation (like a DotSite).
 * 
 * <p>Methods are assumed to be non-blocking (although they may use {@link kilim}
 * for cooperative threading if desired). For objects with blocking methods,
 * use ThreadedObjectProxy.
 * 
 * @author dkitchin
 */
public class ObjectProxy extends Site {
	/**
	 * A Java array being used as an Orc Site.
	 * @author quark
	 */
	private static class ArrayProxy extends DotSite {
		private ObjectProxy proxy;

		public ArrayProxy(ObjectProxy proxy) {
			this.proxy = proxy;
		}

		@Override
		protected void addMethods() {
			addMethod("get", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return Array.get(proxy.instance, args.intArg(0));
				}
			});
			addMethod("set", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					Array.set(proxy.instance, args.intArg(0),
							InvokableHandle.coerce(
									proxy.instance.getClass().getComponentType(),
									args.getArg(1)));
					return signal();
				}
			});
			addMethod("slice", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					Class componentType = proxy.instance.getClass().getComponentType();
					int srcPos = args.intArg(0);
					int length = args.intArg(1) - args.intArg(0);
					Object out = Array.newInstance(componentType, length);
					System.arraycopy(proxy.instance, srcPos, out, 0, length);
					return out;
				}
			});
			addMethod("length", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return Array.getLength(proxy.instance);
				}
			});
		}

		@Override
		public void callSite(Args args, Token caller) throws TokenException {
			try {
				super.callSite(args, caller);
			} catch (MessageNotUnderstoodException _) {
				proxy.callSite(args, caller);
			}
		}
	}
	
	private ClassProxy classProxy;
	private Object instance;
	
	public static Callable proxyFor(Object instance) {
		// we could use a hash map here to reuse proxies but
		// first we should find out if that's actually worthwhile
		if (instance.getClass().isArray()) {
			return new ArrayProxy(new ObjectProxy(instance));
		} else {
			return new ObjectProxy(instance);
		}
	}

	private ObjectProxy(Object instance) {
		this.instance = instance;
		this.classProxy = ClassProxy.forClass(instance.getClass());
	}
	
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		String methodName;
		try {
			methodName = args.fieldName();
		} catch (TokenException e) {
			// If this looks like a site call, call the special method "apply".
			new MethodProxy(instance, classProxy.getMethod("apply"))
				.callSite(args, caller);
			return;
		}
		caller.resume(new MethodProxy(instance, classProxy.getMethod(methodName)));
	}
	
	public Object getProxiedObject() {
		return instance;
	}
}