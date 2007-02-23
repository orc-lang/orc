/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.*;


import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.runtime.nodes.Where;
import orc.runtime.values.Tuple;

/**
 * @author wcook
 *
 * Abstract syntax for "where" expression
 */
public class AsymmetricParallelComposition extends OrcProcess {
	/**
	 * The body in the form
	 * <pre>
	 *     body where bindings
	 * </pre>
	 */
	OrcProcess body;
	
	/**
	 * The bindings in the form
	 * <pre>
	 *     body where bindings
	 * </pre>
	 */
	List<Binding> bindings = new ArrayList<Binding>();

	public AsymmetricParallelComposition(OrcProcess body) {
		this.body = body;
	}

	public void addBinding(String name, OrcProcess item) {
		bindings.add(new Binding(name, item));
	}
	
	/** 
	 * To resolve names in an asymmetric parallel composition, 
	 * add the names of the bindings to the bound variables for the body.
	 * Resolving names in the bindings is somewhat tricky.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		// First, add the names of all the bindings to the bound vals.
		List<String> vals_in_body = new ArrayList<String>();
		List<String> bound_in_body = null;
		vals_in_body.addAll(vals);
		for (Binding b : bindings) {
			vals_in_body.add(b.name);
			if (bound.contains(b.name)){
				if (bound_in_body == null){
					bound_in_body = new ArrayList<String>();
					bound_in_body.addAll(bound);
				}
				bound_in_body.remove(b.name);
			}
		}
		// Then resolve the names in the body and create the answer with that body.
		if (bound_in_body == null)
			bound_in_body = bound;
		OrcProcess bodyRes = body.resolveNames(bound_in_body, vals_in_body);
		AsymmetricParallelComposition answer = new AsymmetricParallelComposition(bodyRes);
		
		// Now iterate through the bindings in reverse order, and make a new binding
		// by resoving the names in the old binding and then adding its name to the list
		// of bound names.
		ListIterator<Binding> iter = bindings.listIterator(bindings.size());
		vals_in_body = new ArrayList<String>();
		vals_in_body.addAll(vals);
		List<Binding> bindingsRes = new ArrayList<Binding>();
		List<String> bound_in_bindings = null;
		while (iter.hasPrevious()) {
		  Binding b = iter.previous();
		  if (bound_in_bindings == null)
		      bindingsRes.add(new Binding(b.name, b.item.resolveNames(bound,vals_in_body)));
		  else 
			  bindingsRes.add(new Binding(b.name, b.item.resolveNames(bound_in_bindings,vals_in_body)));
		  vals_in_body.add(b.name);
		  if (bound.contains(b.name)){
				if (bound_in_bindings == null){
					bound_in_bindings = new ArrayList<String>();
					bound_in_bindings.addAll(bound);
				}
				bound_in_bindings.remove(b.name);
			}
		}
		// Finally, iterate through the list of new bindings in reverse again
		// and add each binding to the answer. This will restore the orginal order.
		iter = bindingsRes.listIterator(bindingsRes.size());
		while (iter.hasPrevious()) {
		  Binding b = iter.previous();
		  answer.addBinding(b.name, b.item);
		}
				
		return answer;
	}

	/**
	 * Compiles the bindings and the body.
	 * Most of the work is done by the bindings. 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		Node result = body.compile(output,defs);
		for (Binding b : bindings) {
			result = b.compile(result,defs);
		}
		return result;
	}
	

	public String toString() {
		return "{" + body + "\nwhere" + Tuple.format(' ', bindings, ";\n   ", '}');
	}

	class Binding {
		public String name;
		public OrcProcess item;
		public Binding(String name, OrcProcess item) {
			this.name = name;
			this.item = item;
		}

		/**
		 * The item is compiled to output its result to a store node.
		 * A Where node is created to run the binding and the body in parallel.
		 * @param base	node of the left side of the where
		 * @return		returns node for complete where expression
		 */
		public Node compile(Node base,List<orc.ast.Definition> defs) {
			return new Where(base, name, item.compile(new Store(name),defs));
		}

		public String toString() {
			return name + " = " + item;
		}
	}
}
