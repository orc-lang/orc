/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import orc.runtime.Args;
import orc.error.JavaException;
import orc.error.MethodTypeMismatchException;
import orc.error.TokenException;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.SimpleSite;
import orc.runtime.values.*;

/**
 * This extends SimpleSite so that methods can use orc.runtime.Continuation to
 * take advantage of Orc's cooperative threading, if desired.
 * 
 * @author dkitchin, quark
 */
public class MethodProxy extends SimpleSite {
    List<Method> wrapped_methods;
    Object self;
    String methodName;
    
    public MethodProxy(List<Method> m, Object self, String name)
    {
        this.wrapped_methods = m;
        this.self = self;
        this.methodName = name;
    }
    
    /* (non-Javadoc)
     * @see orc.runtime.sites.EvalSite#evaluate(java.lang.Object[])
     */
    @Override
    public Value evaluate(Args args) throws TokenException {
        for (Method m : wrapped_methods) {
            try {
            	Object result = m.invoke(self, args.asArray());             	
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
        throw new MethodTypeMismatchException(methodName);
    }
}