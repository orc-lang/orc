package orc.runtime.sites.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import orc.error.JavaException;
import orc.error.MethodTypeMismatchException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.sites.java.ObjectProxy.Delegate;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

public class ThreadedMethodProxy extends ThreadedSite {
	Delegate delegate;
    
    public ThreadedMethodProxy(Delegate delegate) {
    	this.delegate = delegate;
    }

	@Override
	public Value evaluate(Args args) throws TokenException {
        for (Method m : delegate.methods) {
            try {
            	Object result = m.invoke(delegate.that, args.asArray());             	
                if (m.getReturnType().getName().equals("void")) {
                    // if return type is void, return signal
                	return Value.signal();
                } else {
                	// otherwise wrap Java value as Orc constant
                    return new Constant(result);
                }
            } catch (IllegalArgumentException e) {
            	// continue looking for a matching method
            } catch (IllegalAccessException e) {
            	throw new JavaException(e);
			} catch (InvocationTargetException e) {
            	throw new JavaException(e);
			}
        }
        //throw new TokenException("Argument types did not match any implementation for method '" + methodName + "'.");
        throw new MethodTypeMismatchException(delegate.name);
	}
}
