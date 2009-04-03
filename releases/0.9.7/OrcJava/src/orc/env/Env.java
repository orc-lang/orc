/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.env;

import orc.ast.simple.arg.Var;
import orc.error.OrcError;
import orc.runtime.values.Future;
import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

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
		private Binding<T> parent;
		private T value;
		public Binding(Binding<T> parent, T value) {
			this.parent = parent;
			this.value = value;
		}
	}

	/** Copy constructor */
	private Env(Binding<T> head) {
		this.head = head;
	}
	
	public Env() {
		this(null);
	}

	/** Push one item. */
	public void add(T item) {
		head = new Binding(head, item);
	}
	
	/** Push multiple items, in the order they appear in the list. */
	public void addAll(List<T> items) {
		for (T item : items) add(item);
	}
	
	/** Return a list of items in the order they were pushed. */
	public List<T> items() {
		LinkedList<T> out = new LinkedList<T>();
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
	public int search(T target) throws SearchFailureException {
		Binding<T> node = head;
		for (int index = 0; node != null; ++index, node = node.parent) {
			if (target.equals(node.value)) return index;
		}
		throw new SearchFailureException("Target item " + target + " not found in environment");
	}

	/** Pop n items. */
	public void unwind(int n) {
		for (; n > 0; --n) head = head.parent;
	}
	
	/**
	 * Create an independent copy of the environment.
	 */
	public Env<T> clone() {
		return new Env(head);
	}
	
	/**
	 * Create an independent copy of the environment, extended with a new item.
	 */
	public Env<T> extend(T item) {
		return new Env(new Binding(head, item));
	}
}