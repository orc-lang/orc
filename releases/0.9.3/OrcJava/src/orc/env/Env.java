/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.env;

import orc.ast.simple.arg.Var;
import orc.error.OrcError;
import orc.runtime.values.Future;
import java.io.*;
import java.util.List;

/**
 * Generic indexed environment, used primarily at runtime. 
 * Env is also content addressable, so it can be used for the
 * deBruijn index conversion in the compiler.
 *   
 * @author dkitchin
 */
public class Env<T> implements Serializable {
	
	ENode node;

	// Environment given a specific node
	protected Env(ENode node) {
		this.node = node;
	}
	
	public Env() {
		this.node = null;
	}

	public Env<T> add(T item) {
		return new Env<T>(new ENode(node, item));
	}
	
	public Env<T> addAll(List<T> items) {
		
		ENode here = node;
		
		for(T item : items) {
			here = new ENode(here, item);
		}
		
		return new Env<T>(here);
	}
	
	/**
	 * Lookup a variable in the environment
	 * Currently uses a linear search.
	 * @param   index  Stack depth (a deBruijn index)
	 * @return  The bound item
	 */
	public T lookup(int index) {
		
		if (index < 0) {
			// this should be impossible
			throw new OrcError("Invalid argument to lookup, index " + index + " is negative.");
		}
		
		for(ENode here = node; here != null; here = here.parent, index--) {
			if (index == 0) {
				return here.item;
			}
		}
		
		throw new OrcError("Unbound variable.");
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
		
		int depth = 0;
		
		for(ENode here = node; here != null; here = here.parent, depth++) {
			if (target.equals(here.item)) {
				return depth;
			}
		}

		// this should be impossible, because any variable which
		// is not in scope should have been created as a NamedVar,
		// not a Var
		throw new OrcError("Target " + target + " not found in environment; can't return index.");

	}

	
	/**
	 * Individual entries in the environment.
	 */ 
	protected class ENode {
		
		T item;
		ENode parent;
		
		ENode(ENode parent, T item) {
			this.parent = parent;
			this.item = item;
		}
	}

	public Env<T> unwind(int width) {
		
		ENode here = node;
		for(int i = 0; i < width; i++) {
			here = here.parent;
		}
		return new Env<T>(here);
	}
	
}
