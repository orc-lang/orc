/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import orc.error.JavaException;
import orc.error.MessageNotUnderstoodException;
import orc.error.MethodTypeMismatchException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.*;


/**
 * @author dkitchin, quark
 */
public class ClassProxy extends EvalSite {
	private static final long serialVersionUID = 1L;
	Class wrapped_class;

	public ClassProxy(Class c) {
		this.wrapped_class = c;
	}

	@Override
	public Value evaluate(Args args) throws TokenException {
		
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
			List<Method> matching_methods = new LinkedList<Method>();
			for (Method m : wrapped_class.getMethods()) {
				if (m.getName().equals(methodName)) {
					matching_methods.add(m);
				}
			}

			if (matching_methods.isEmpty()) {
				throw new MessageNotUnderstoodException(methodName);
			}

			return new MethodProxy(matching_methods, null, methodName);
		}


		Object inst = null;

		// Attempt to construct an instance of the class, using the site call args as parameters
		// Try each of the possible constructors until one matches.
		for (Constructor cn : wrapped_class.getConstructors()) {
			try {
				inst = cn.newInstance(args.asArray());
			}
			catch (IllegalArgumentException e) {}
			catch (Exception e) {
				throw new JavaException(e);
			}
		}

		// It's possible that none of the constructors worked; this is a runtime error.
		if (inst == null) {	
			throw new MethodTypeMismatchException("constructor"); 
		}
		
		// create a proxy for the object
		return new Constant(inst);
	}
}