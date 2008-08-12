package orc.runtime.sites.java;

import java.lang.reflect.Constructor;

import orc.error.runtime.JavaException;
import orc.error.runtime.MethodTypeMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.java.ObjectProxy.DelegateCache;


/**
 * @author dkitchin, quark
 */
public class ClassProxy extends EvalSite {
	private static final long serialVersionUID = 1L;
	private Class wrapped_class;
	private DelegateCache delegates;
	

	public ClassProxy(Class c) {
		this.wrapped_class = c;
		this.delegates = new DelegateCache(c, null);
	}

	@Override
	public Object evaluate(Args args) throws TokenException {
		
		// If this looks like a field reference, assume it is a call to a static
		// method and treat it accordingly. That means you can't call a
		// constructor with a field as the first argument, which is OK since it
		// is impossible to create a bare field literal in Orc anyways.
		String methodName = null;
		try {
			methodName = args.fieldName(); 
		} catch (TokenException e) {
			// do nothing
		}
		if (methodName != null) {
			return new MethodProxy(delegates.get(methodName));
		}

		Object inst = null;

		// Attempt to construct an instance of the class, using the site call args as parameters
		// Try each of the possible constructors until one matches.
		for (Constructor cn : wrapped_class.getConstructors()) {
			try {
				inst = cn.newInstance(args.asArray());
			} catch (IllegalArgumentException e) {
				// if the arguments didn't match, try the
				// next constructor
			} catch (Exception e) {
				throw new JavaException(e);
			}
		}

		// It's possible that none of the constructors worked; this is a runtime error.
		if (inst == null) {	
			throw new MethodTypeMismatchException("constructor"); 
		}
		
		// create a proxy for the object
		return inst;
	}
}