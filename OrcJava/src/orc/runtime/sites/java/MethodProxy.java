/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import kilim.Fiber;
import kilim.State;
import kilim.Task;
import kilim.pausable;
import orc.error.JavaException;
import orc.error.MethodTypeMismatchException;
import orc.error.SiteException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Kilim;
import orc.runtime.Token;
import orc.runtime.Kilim.Pausable;
import orc.runtime.sites.Site;
import orc.runtime.sites.java.ObjectProxy.Delegate;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

/**
 * Allow a Java method to be used as an Orc site.
 * 
 * <p>MAGIC: pausible methods are run in a Kilim task.
 * We actually go to some lengths to avoid running
 * non-pausable methods in a Kilim task to ensure that
 * if this site is wrapped by ThreadSite, the method
 * will actually run in its own thread and not in the
 * Kilim thread.
 * 
 * @author quark, dkitchin
 */
public class MethodProxy extends Site {
	Delegate delegate;
    
    public MethodProxy(Delegate delegate) {
    	this.delegate = delegate;
    }

	@Override
	public void callSite(Args args, Token caller) throws TokenException {
    	Object[] oargs = args.asArray();
    	lookingForMethod:
        for (Method m : delegate.methods) {
        	Class[] parameterTypes = m.getParameterTypes();
        	// skip methods with the wrong number of arguments
        	// FIXME: support varargs
        	if (parameterTypes.length != oargs.length)
        		continue;
        	// check argument types
        	for (int i = 0; i < parameterTypes.length; ++i) {
        		if (oargs[i] == null) continue;
        		// FIXME: does not account for implicit numeric conversions
        		if (!parameterTypes[i].isAssignableFrom(oargs[i].getClass())) {
        			continue lookingForMethod;
        		}
        	}
        	Object result;
            if (!m.isAnnotationPresent(pausable.class)) {
            	// non-pausable methods should be invoked directly
            	invoke(caller, m, delegate.that, oargs);
            } else {
            	// pausable methods are invoked within a Kilim task
            	invokePausable(caller, m, delegate.that, oargs);
            }
        	return;
        }
        throw new MethodTypeMismatchException(delegate.name);
    }
	
    public static void invoke(Token caller, Method m, Object that, Object[] args) {
		try {
    		caller.resume(wrapObject(m, m.invoke(that, args)));
		} catch (Exception e) {
			caller.error(new JavaException(e));
		}
    }
    
    private static Value wrapObject(Method m, Object o) {
        if (m.getReturnType().getName().equals("void"))
        	return Value.signal();
        else return new Constant(o);
    }
    
    public static void invokePausable(Token caller,
    		Method m, final Object that, final Object[] args) {
        // Find the woven method
        final Method pm;
        Class[] parameters = m.getParameterTypes();
        Class[] pparameters = new Class[parameters.length+1];
        for (int i = 0; i < parameters.length; ++i) pparameters[i] = parameters[i];
        pparameters[pparameters.length-1] = Fiber.class;
        try {
            pm = m.getDeclaringClass().getMethod(m.getName(), pparameters);
        } catch (NoSuchMethodException e) {
        	caller.error(new SiteException("Unwoven method: " + m));
        	return;
        }
        final Object[] pargs = new Object[args.length+1]; // +1 for the Fiber
        for (int i = 0; i < args.length; ++i) pargs[i] = args[i];
        
        // Invoke the method inside a task
        Kilim.runPausable(caller, new Pausable<Value>() {
        	public @pausable Value call() throws Exception {
        		try {
	                return wrapObject(pm, _invokePausable(pm, that, pargs));
        		} catch (InvocationTargetException e) {
        			// attempt to unwrap a reflected exception
        			Throwable cause = e.getCause();
        			if (cause instanceof Exception)
        				throw (Exception)cause;
        			if (cause instanceof RuntimeException)
        				throw (RuntimeException)cause;
        			throw e;
        		}
        	}
        });
    }
    
    /**
     * Invoke a possibly-pausable method reflectively.
     * The weaver translates calls to this into calls to
     * {@link Task#_invokePausable(Method, Object, Object[], Fiber)}
     */
    @pausable
    public static Object _invokePausable(Method m, Object that, Object[] args)
    throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    	throw new AssertionError("Unwoven method: public static Object MethodProxy#_invokePausable(Method, Object, Object[])");
    }
    
    /**
	 * Hand-woven implementation of pausable invocation. This is necessary to
	 * weave the fiber into the reflective invocation.
	 * 
	 * <p>
	 * FIXME: Kilim gives an error when I make this private: Error weaving
	 * build. orc.runtime.sites.java.MethodProxy.access$0(
	 * Ljava/lang/reflect/Method;Ljava/lang/Object;[
	 * Ljava/lang/Object;)Ljava/lang/Object;
	 * should be marked pausable. It calls pausable methods
	 */
    @SuppressWarnings("unused")
	public static Object _invokePausable(Method m, Object that, Object[] args, Fiber f)
    throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (f.pc == 1) {
            // resuming
            InvokeState state = (InvokeState)f.curState;
            m = state.m;
            that = state.that;
            args = state.args;
        }
        args[args.length-1] = f.down();
        Object out = m.invoke(that, args);
        if (f.up() == 2) { // Fiber.PAUSING__NO_STATE
        	InvokeState state = new InvokeState();
            state.pc = 1;
            state.m = m;
            state.that = that;
            state.args = args;
            f.setState(state);
        }
        return out;
    }
    
    /**
     * Pause state for invocation.
     */
    private static class InvokeState extends State {
        public Method m;
        public Object that;
        public Object[] args;
    }
}