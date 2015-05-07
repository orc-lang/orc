/**
 * 
 */
package orc.runtime.sites.java;

import java.lang.reflect.Constructor;

public class ConstructorHandle extends InvokableHandle<Constructor> {
	public ConstructorHandle(Constructor[] constructors) {
		super("<init>", constructors);
	}
	
	protected Class[] getParameterTypes(Constructor c) {
		return c.getParameterTypes();
	}
	
	protected int getModifiers(Constructor c) {
		return c.getModifiers();
	}
}