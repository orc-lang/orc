/**
 * 
 */
package orc.runtime.sites.java;


import orc.error.runtime.TokenException;
import orc.lib.util.ThreadSite;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

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
	private static final long serialVersionUID = 1L;
	private ClassProxy classProxy;
	private Object instance;
	
	public static ObjectProxy proxyFor(Object instance) {
		// we could use a hash map here but first we should
		// find out if that's actually worthwhile
		return new ObjectProxy(instance);
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