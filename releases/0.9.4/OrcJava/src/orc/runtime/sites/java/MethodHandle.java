package orc.runtime.sites.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.MethodTypeMismatchException;

/**
 * Java has method overloading so this may actually call one of several methods
 * depending on the number and type of arguments. This should be cached so that
 * only one instance is used for a given method name and class.
 * 
 * @author quark
 */
public class MethodHandle extends InvokableHandle<Method> {
	public MethodHandle(String name, Method[] methods) {
		super(name, methods);
	}
	
	public Class[] getParameterTypes(Method m) {
		return m.getParameterTypes();
	}
	
	protected int getModifiers(Method m) {
		return m.getModifiers();
	}
}
