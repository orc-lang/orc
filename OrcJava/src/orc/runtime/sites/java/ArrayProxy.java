package orc.runtime.sites.java;

import java.lang.reflect.Array;

import orc.error.runtime.JavaException;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;

/**
 * A Java array being used as an Orc Site.
 * @author quark
 */
class ArrayProxy {
	private Object instance;

	public ArrayProxy(Object instance) {
		this.instance = instance;
	}
	
	public Object apply(final int index) {
		// check bounds here rather than when the reference is used,
		// for easier debugging (but it's less efficient)
		if (index < 0 || index >= Array.getLength(instance)) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		// FIXME: we need a reference interface
		return new Object() {
			public Object read() {
				return get(index);
			}
			public void write(Object value) {
				set(index, value);
			}
		};
	}
	
	public Object get(int index) {
		return Array.get(instance, index);
	}
	
	public void set(int index, Object value) {
		Array.set(instance, index,
				InvokableHandle.coerce(
						instance.getClass().getComponentType(),
						value));
	}
	
	public Object slice(int lower, int upper) {
		Class componentType = instance.getClass().getComponentType();
		int length = upper - lower;
		Object out = Array.newInstance(componentType, length);
		System.arraycopy(instance, lower, out, 0, length);
		return out;
	}
	
	public void fill(Object value) {
		// NB: we cannot use Arrays.fill because
		// we don't know the type of the array
		int length = Array.getLength(instance);
		for (int i = 0; i < length; ++i) {
			Array.set(instance, i, value);
		}
	}
	
	public int length() {
		return Array.getLength(instance);
	}
}