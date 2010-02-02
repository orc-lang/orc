//
// Env.java -- Java class Env
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.env;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Generic indexed environment, used primarily at runtime. 
 * 
 * Env is also content addressable, so it can be used for
 * deBruijn index conversions in the compiler.
 * 
 * Env allows null values, a capability used in the typechecker
 * to distinguish bound types from free ones.
 * 
 * <p>Currently this is implemented as a simple linked-list
 * of bindings, which provides O(n) lookups and O(1) copies.
 * This sounds inefficient, but in practice turns out to be
 * just as good or better than more complicated schemes
 * for O(1) lookup.
 * 
 * @author dkitchin, quark
 */
public final class Env<T> implements Serializable, Cloneable {
	private Binding<T> head;

	/** Binding in the stack. */
	private static final class Binding<T> {
		private final Binding<T> parent;
		private final T value;

		public Binding(final Binding<T> parent, final T value) {
			this.parent = parent;
			this.value = value;
		}
	}

	/** Copy constructor */
	private Env(final Binding<T> head) {
		this.head = head;
	}

	public Env() {
		this(null);
	}

	/** Push one item. */
	public void add(final T item) {
		head = new Binding(head, item);
	}

	/** Push multiple items, in the order they appear in the list. */
	public void addAll(final List<T> items) {
		for (final T item : items) {
			add(item);
		}
	}

	/** Return a list of items in the order they were pushed. */
	public List<T> items() {
		final LinkedList<T> out = new LinkedList<T>();
		for (Binding<T> node = head; node != null; node = node.parent) {
			out.addFirst(node.value);
		}
		return out;
	}

	/**
	 * Look up a variable's value in the environment.
	 * @param   index  Stack depth (a deBruijn index)
	 * @return  The bound item
	 * @throws LookupFailureException 
	 */
	public T lookup(int index) throws LookupFailureException {
		Binding<T> node = head;
		for (; index > 0 && node != null; --index, node = node.parent) {
		}
		if (node == null) {
			throw new LookupFailureException();
		}
		return node.value;
	}

	/**
	 * Content-addressable mode. Used in compilation
	 * to determine the deBruijn indices from an
	 * environment populated by variables in a different
	 * representation.
	 * 
	 * Assuming no error is raised, search and lookup are inverses: 
	 *   search(lookup(i)) = i
	 *   lookup(search(o)) = o
	 * 
	 * @param target  The item 
	 * @return        The index of the target item
	 * @throws SearchFailureException 
	 */
	public int search(final T target) throws SearchFailureException {
		Binding<T> node = head;
		for (int index = 0; node != null; ++index, node = node.parent) {
			if (target.equals(node.value)) {
				return index;
			}
		}
		throw new SearchFailureException("Target item " + target + " not found in environment");
	}

	/** Pop n items. */
	public void unwind(int n) {
		for (; n > 0; --n) {
			head = head.parent;
		}
	}

	/**
	 * Create an independent copy of the environment.
	 */
	@Override
	public Env<T> clone() {
		return new Env(head);
	}

	/**
	 * Create an independent copy of the environment, extended with a new item.
	 */
	public Env<T> extend(final T item) {
		return new Env(new Binding(head, item));
	}

	/**
	 * Create an independent copy of the environment, extended with a list of new item.
	 * The extensions occur in list order, so the last item of the list will be the
	 * most recent binding.
	 */
	public Env<T> extendAll(final List<T> items) {
		Env<T> env = this;

		for (final T item : items) {
			env = env.extend(item);
		}

		return env;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName() + " [");
		for (Binding<T> currHead = head; currHead != null; currHead = currHead.parent) {
			sb.append(currHead.value);
			if (currHead.parent != null) {
				sb.append(", ");
			}
		}
		return sb.append(']').toString();
	}
}
