//
// ArrayProxy.java -- Java class ArrayProxy
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.sites.java;

import java.lang.reflect.Array;

import orc.runtime.values.Reference;

/**
 * A Java array being used as an Orc Site.
 * @author quark
 */
class ArrayProxy<E> {
	private final Object instance;

	public ArrayProxy(final Object instance) {
		this.instance = instance;
	}

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

			public void write(final E value) {
				set(index, value);
			}
		};
	}

	public E get(final int index) {
		return (E) Array.get(instance, index);
	}

	public void set(final int index, final E value) {
		Array.set(instance, index, InvokableHandle.coerce(instance.getClass().getComponentType(), value));
	}

	public Object slice(final int lower, final int upper) {
		final Class componentType = instance.getClass().getComponentType();
		final int length = upper - lower;
		final Object out = Array.newInstance(componentType, length);
		System.arraycopy(instance, lower, out, 0, length);
		return out;
	}

	public void fill(final E value) {
		// NB: we cannot use Arrays.fill because
		// we don't know the type of the array
		final int length = Array.getLength(instance);
		for (int i = 0; i < length; ++i) {
			Array.set(instance, i, value);
		}
	}

	public int length() {
		return Array.getLength(instance);
	}
}
