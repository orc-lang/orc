package orc.runtime.sites.java;

import java.lang.reflect.Array;

import orc.error.runtime.JavaException;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.values.Reference;

/**
 * A Java array being used as an Orc Site.
 * @author quark
 */
class ArrayProxy<E> {
	private Object instance;

	public ArrayProxy(Object instance) {
		this.instance = instance;
	}
	
	@SuppressWarnings("unused")
	public Reference<E> apply(final int index) {
		// check bounds here rather than when the reference is used,
		// for easier debugging (but it's less efficient)
		if (index < 0 || index >= Array.getLength(instance)) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		return new Reference<E>() {
			public E read() {
				return get(index);
			}
			public void write(E value) {
				set(index, value);
			}
		};
	}
	
	public E get(int index) {
		return (E)Array.get(instance, index);
	}
	
	public void set(int index, E value) {
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
	
	public void fill(E value) {
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