package orc.runtime.sites.java;

import java.lang.reflect.Field;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.lib.util.ThreadSite;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * Objects whose methods should always be called in new threads. This is to be
 * avoided if possible, but it's safer than allowing native methods to block the
 * interpreter. The main reason you might need this is when wrapping some other
 * proxy, like a webservices proxy.
 * 
 * @author quark
 */
public class ThreadedObjectProxy extends Site {
	private Object instance;
	private ClassProxy classProxy;
	public ThreadedObjectProxy(Object instance) {
		this.instance = instance;
		this.classProxy = ClassProxy.forClass(instance.getClass());
	}
	public void callSite(final Args args, final Token caller) throws TokenException {
		String member;
		try {
			member = args.fieldName();
		} catch (TokenException e) {
			// If this looks like a site call, call the special method "apply".
			ThreadSite.makeThreaded(new MethodProxy(instance,
					classProxy.getMethod(caller, "apply")))
				.callSite(args, caller);
			return;
		}
		try {
			// try and return a method handle
			caller.resume(ThreadSite.makeThreaded(
					new MethodProxy(instance, classProxy.getMethod(caller, member))));
		} catch (MessageNotUnderstoodException e) {
			try {
				// if a method was not found, return a field value
				caller.resume(classProxy.getField(caller, member).get(instance));
			} catch (IllegalAccessException _) {
				throw e;
			} catch (NoSuchFieldException _) {
				throw e;
			}
		}
	}
}
