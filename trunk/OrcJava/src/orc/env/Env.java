/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.env;

import orc.ast.simple.arg.Var;
import orc.error.OrcError;
import orc.runtime.values.Future;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Generic indexed environment, used primarily at runtime. 
 * Env is also content addressable, so it can be used for the
 * deBruijn index conversion in the compiler.
 * 
 * <p>The implementation uses a linked list of stacks, where
 * only the head is mutated. This means the tail can be shared,
 * saving memory and copying time relative to a stack implementation,
 * but within each stack lookup is O(1), saving lookup time relative
 * to a linked list implementation.
 * 
 * @author dkitchin, quark
 */
public final class Env<T> implements Serializable, Cloneable {
	private Env<T> parent;
	private Stack<T> stack;

	/** Copy constructor */
	protected Env(Env<T> parent, Stack<T> stack) {
		this.parent = parent;
		this.stack = stack;
	}
	
	public Env(Env<T> parent) {
		this.parent = parent;
		this.stack = new Stack<T>();
	}
	
	public Env() {
		this(null);
	}

	public void add(T item) {
		stack.push(item);
	}
	
	private void collect(LinkedList<T> items) {
		if (parent != null) parent.collect(items);
		for (T item : stack) items.add(item);
	}
	
	/** Return a list of items in the order they were added. */
	public List<T> items() {
		LinkedList<T> out = new LinkedList<T>();
		collect(out);
		return out;
	}
	
	public void addAll(List<T> items) {
		stack.addAll(items);
	}
	
	/**
	 * Lookup a variable in the environment
	 * @param   index  Stack depth (a deBruijn index)
	 * @return  The bound item
	 */
	public T lookup(int index) {
		index -= stack.size();
		if (index >= 0) {
			// will be found in the parent
			assert(parent != null);
			return parent.lookup(index);
		} else {
			// found here
			return stack.get(-index - 1);
		}
	}
	
	/**
	 * Content addressable mode. Used in compilation
	 * to determine the deBruijn indices from an
	 * environment populated by Var objects.
	 * 
	 * Assuming no error is raised, search and lookup are inverses: 
	 *   search(lookup(i)) = i
	 *   lookup(search(o)) = o
	 * 
	 * @param target  The item 
	 * @return        The index of the target item
	 */
	public int search(T target) {
		int depth = stack.search(target);
		if (depth > 0) {
			// found in the stack;
			// search returns a 1-based index but we
			// assume 0-based.
			return depth - 1;
		} else {
			assert(parent != null);
			return parent.search(target) + stack.size();
		}
	}

	public void unwind(int depth) {
		assert(depth <= stack.size());
		// pop values off the stack; unwinding past
		// the parent is not allowed
		for (int i = 0; i < depth; i++) stack.pop();
	}
	
	public String toString() {
		return items().toString();
	}
	
	public Env<T> clone() {
		// Various strategies are possible here, which trade
		// off copying time for lookup speed. For example, when
		// an environment is copied, we could move it to a shared
		// parent node so it doesn't have to be copied again. But
		// it turns out empirically this extra work isn't worth it.
		return new Env(parent, (Stack<T>)stack.clone());
	}
}