//
// DefsDeclaration.java -- Java class DefsDeclaration
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

package orc.ast.extended.declaration;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import orc.ast.extended.declaration.def.AggregateDef;
import orc.ast.extended.declaration.def.DefMember;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.visitor.Visitor;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Def;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

/**
 * A group of defined expressions, together as a declaration. 
 * 
 * Any contiguous sequence of definitions is assumed to be mutually recursive.
 * 
 * The simplification of a group of definitions is complicated by the mutually
 * recursive binding, which requires that each definition bind its name in all of
 * the other definitions.
 * 
 * @author dkitchin
 */

public class DefsDeclaration extends Declaration {

	public List<DefMember> defs;

	public DefsDeclaration(final List<DefMember> defs) {
		this.defs = defs;
	}

	@Override
	public orc.ast.simple.expression.Expression bindto(final orc.ast.simple.expression.Expression target) throws CompilationException {

		final Map<String, AggregateDef> dmap = new TreeMap<String, AggregateDef>();

		// Aggregate all of the definitions in the list into the map
		for (final DefMember d : defs) {
			final String name = d.name;
			if (!dmap.containsKey(name)) {
				dmap.put(name, new AggregateDef());
			}
			d.extend(dmap.get(name));
		}

		// Associate the names of the definitions with their bound variables
		final Map<FreeVariable, Variable> vmap = new TreeMap<FreeVariable, Variable>();

		for (final Entry<String, AggregateDef> e : dmap.entrySet()) {
			final FreeVariable x = new FreeVariable(e.getKey());
			final Variable v = e.getValue().getVar();
			vmap.put(x, v);
		}

		// Create the new list of simplified definitions,
		// with their names mutually bound.

		final List<orc.ast.simple.expression.Def> newdefs = new LinkedList<orc.ast.simple.expression.Def>();

		for (final AggregateDef d : dmap.values()) {
			final Def newd = d.simplify().subMap(vmap);
			newdefs.add(newd);
		}

		// Bind all of these definition names in their scope
		final orc.ast.simple.expression.Expression newtarget = target.subMap(vmap);

		// Partition the list of definitions into mutually recursive groups
		final List<List<Def>> defparts = defpartition(newdefs);

		// Bind the definitions onto the target expression in the correct order
		orc.ast.simple.expression.Expression result = newtarget;
		for (final List<Def> part : defparts) {

			/* If no member of the set of definitions is ever mentioned in the target,
			 * do not bind them. This prevents the AST from becoming bloated
			 * with unused code.
			 */
			boolean used = false;
			for (final Def d : part) {
				if (result.vars().contains(d.name)) {
					used = true;
					break;
				}
			}
			if (used) {
				result = new orc.ast.simple.expression.DeclareDefs(part, result);
			}

		}

		// Attach a source location to the whole expression and return it
		return new WithLocation(result, getSourceLocation());
	}

	// Partition a list of definitions into mutually recursive sublists
	// The list of lists is returned in reverse scope order; a definition
	// name that appears in one sublist may not occur as a free variable in
	// a later sublist.
	private List<List<Def>> defpartition(final List<Def> defs) {

		final List<Node<Def>> graph = new LinkedList<Node<Def>>();

		for (final Def d : defs) {
			graph.add(new Node<Def>(d));
		}

		for (final Node<Def> n : graph) {
			final Set<Variable> nvars = n.item.vars();

			for (final Node<Def> m : graph) {

				if (n == m) {
					continue;
				}

				/* Draw an edge from N to M if the name of definition M
				 * appears in the free vars of definition N; this means
				 * that N depends on M.
				 */
				if (nvars.contains(m.item.name)) {
					n.connectTo(m);
				}
			}
		}

		/*
		 * Perform a forward depth-first search on the graph to mark
		 * it with finishing times. We'll ignore the forest it generates.
		 * Then, sort the graph according to these finishing times.
		 */
		final DFS dfs = new DFS(DFS.direction.FORWARD);
		dfs.search(graph);
		Collections.sort(graph, new NodeComparator<Def>());

		/*
		 * Now, run a backward DFS based on the sorted order given
		 * by the forward DFS. The forest that this DFS generates
		 * will consist of the strongly connected components of the
		 * graph, sorted topologically.
		 * 
		 * These correspond to the mutually recursive definition groups,
		 * sorted by dependency.
		 */
		final DFS backdfs = new DFS(DFS.direction.BACKWARD);
		final List<List<Node<Def>>> forest = backdfs.search(graph);

		/*
		 * Extract the groups of definitions from the groups of nodes and return them. 
		 */
		final List<List<Def>> results = new LinkedList<List<Def>>();
		for (final List<Node<Def>> tree : forest) {
			final List<Def> group = new LinkedList<Def>();
			for (final Node<Def> n : tree) {
				group.add(n.item);
			}
			results.add(group);
		}
		return results;
	}

	@Override
	public String toString() {
		return Expression.join(defs, "\n");
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

class Node<T> {

	public T item;
	public Integer vistime = null;
	public Integer fintime = null;
	public List<Node<T>> succ;
	public List<Node<T>> pred;

	public Node(final T item) {
		this.item = item;
		this.succ = new LinkedList<Node<T>>();
		this.pred = new LinkedList<Node<T>>();
	}

	public void clean() {
		vistime = null;
		fintime = null;
	}

	public void visit(final int t) {
		vistime = t;
	}

	public void finish(final int t) {
		fintime = t;
	}

	public void connectTo(final Node<T> that) {
		succ.add(that);
		that.connectFrom(this);
	}

	protected void connectFrom(final Node<T> that) {
		pred.add(that);
	}

}

class DFS {

	public static enum direction {
		FORWARD, BACKWARD
	};

	int time = 0;
	direction dir;

	public DFS(final direction dir) {
		this.dir = dir;
	}

	/*
	 * Erase all time markers on the given nodes,
	 * then perform a DFS in the indicated direction on these nodes,
	 * marking them with visit times and finish times.
	 * 
	 * When there is a choice about visiting order, visit the nodes 
	 * in the order given by the graph list.
	 * 
	 * cf. Introduction to Algorithms (CLRS)
	 * 
	 * Return a list of lists of nodes, consisting of the path of
	 * nodes visited in the DFS.
	 */
	public <T> List<List<Node<T>>> search(final List<Node<T>> graph) {

		for (final Node<T> n : graph) {
			n.clean();
		}

		time = 0;

		final List<List<Node<T>>> forest = new LinkedList<List<Node<T>>>();

		for (final Node<T> n : graph) {
			if (n.vistime == null) {
				final List<Node<T>> tree = new LinkedList<Node<T>>();
				visit(n, tree);
				forest.add(tree);
			}
		}

		return forest;
	}

	public <T> void visit(final Node<T> n, final List<Node<T>> tree) {

		n.visit(++time);
		tree.add(n);
		for (final Node<T> m : dir == direction.FORWARD ? n.succ : n.pred) {
			if (m.vistime == null) {
				visit(m, tree);
			}
		}
		n.finish(++time);
	}

}

class NodeComparator<T> implements Comparator<Node<T>> {

	public int compare(final Node<T> o1, final Node<T> o2) {
		return o2.fintime.compareTo(o1.fintime);
	}
}
