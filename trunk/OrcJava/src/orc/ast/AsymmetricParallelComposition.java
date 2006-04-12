/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.ArrayList;
import java.util.List;

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
	 * Compiles the bindings and the body.
	 * Most of the work is done by the bindings. 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		Node result = body.compile(output);
		for (Binding b : bindings) {
			result = b.compile(result);
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
		public Node compile(Node base) {
			return new Where(base, name, item.compile(new Store(name)));
		}

		public String toString() {
			return name + " = " + item;
		}
	}
}
