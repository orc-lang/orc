package orc.ast.extended.declaration;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.DFS.direction;
import orc.ast.extended.declaration.def.AggregateDef;
import orc.ast.extended.declaration.def.Clause;
import orc.ast.extended.declaration.def.DefMember;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.expression.Expression;
import orc.ast.simple.argument.*;
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
	
	public DefsDeclaration(List<DefMember> defs)
	{
		this.defs = defs;
	}
	
	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) throws CompilationException {
		
		
		Map<String, AggregateDef> dmap = new TreeMap<String, AggregateDef>(); 
		
		// Aggregate all of the definitions in the list into the map
		for (DefMember d : defs) {
			String name = d.name;
			if (!dmap.containsKey(name)) {
				dmap.put(name, new AggregateDef());
			}
			d.extend(dmap.get(name));
		}
		
		// Associate the names of the definitions with their bound variables
		Map<FreeVariable, Variable> vmap = new TreeMap<FreeVariable, Variable>();
		
		for (Entry<String, AggregateDef> e : dmap.entrySet()) {
			FreeVariable x = new FreeVariable(e.getKey());
			Variable v = e.getValue().getVar();
			vmap.put(x,v);
		}
		
		// Create the new list of simplified definitions,
		// with their names mutually bound.
		
		List<orc.ast.simple.expression.Def> newdefs = new LinkedList<orc.ast.simple.expression.Def>();
		
		for (AggregateDef d : dmap.values()) {
			Def newd = d.simplify().subMap(vmap);
			newdefs.add(newd);
		}
		
		// Bind all of these definition names in their scope
		orc.ast.simple.expression.Expression newtarget = target.subMap(vmap);		
		
		// Partition the list of definitions into mutually recursive groups,
		// and bind them onto the target expression in the correct order
		List<List<Def>> defparts = defpartition(newdefs);
		
		orc.ast.simple.expression.Expression result = newtarget;
		for (List<Def> part : defparts) {
			result = new orc.ast.simple.expression.DeclareDefs(part, result);
		}
		
		// Attach a source location to the whole expression and return it
		return new WithLocation(result, getSourceLocation());
	}
	
	
	// Partition a list of definitions into mutually recursive sublists
	// The list of lists is returned in reverse scope order; a definition
	// name that appears in one sublist may not occur as a free variable in
	// a later sublist.
	private List<List<Def>> defpartition(List<Def> defs) {
		
		List<Node<Def>> graph = new LinkedList<Node<Def>>();
		
		for(Def d : defs) {
			graph.add(new Node<Def>(d));
		}
		
		for (Node<Def> n : graph) {
			Set<Variable> nvars = n.item.vars();
			
			for (Node<Def> m : graph) {
				
				if (n == m) { continue; }
				
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
		DFS dfs = new DFS(DFS.direction.FORWARD);
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
		DFS backdfs = new DFS(DFS.direction.BACKWARD);
		List<List<Node<Def>>> forest = backdfs.search(graph);
		
		
		/*
		 * Extract the groups of definitions from the groups of nodes and return them. 
		 */
		List<List<Def>> results = new LinkedList<List<Def>>();		
		for(List<Node<Def>> tree : forest) {
			List<Def> group = new LinkedList<Def>();
			for (Node<Def> n : tree) {
				group.add(n.item);
			}
			results.add(group);
		}		
		return results;
	}

	public String toString() {
		return Expression.join(defs, "\n");
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
 


class Node<T> {
	
	public T item;
	public Integer vistime = null;
	public Integer fintime = null;
	public List<Node<T>> succ;
	public List<Node<T>> pred;

	public Node(T item) {
		this.item = item;
		this.succ = new LinkedList<Node<T>>();
		this.pred = new LinkedList<Node<T>>();
	}
	
	public void clean() {
		vistime = null;
		fintime = null;
	}
	
	public void visit(int t) {
		vistime = t;
	}
	
	public void finish(int t) {
		fintime = t;
	}
	
	
	public void connectTo(Node<T> that) {
		succ.add(that);
		that.connectFrom(this);
	}

	protected void connectFrom(Node<T> that) {
		pred.add(that);
	}
	
}

class DFS {

	public static enum direction {FORWARD, BACKWARD};
	int time = 0;
	direction dir;
	
	public DFS(direction dir) {
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
	public <T> List<List<Node<T>>> search(List<Node<T>> graph) {
	
		for (Node<T> n : graph) {
			n.clean();
		}
		
		time = 0;
		
		List<List<Node<T>>> forest = new LinkedList<List<Node<T>>>();
		
		for (Node<T> n : graph) {
			if (n.vistime == null) {
				List<Node<T>> tree = new LinkedList<Node<T>>();
				visit(n, tree);
				forest.add(tree);
			}
		}
		
		return forest;
	}
	
	public <T> void visit(Node<T> n, List<Node<T>> tree) {
		
		n.visit(++time);
		tree.add(n);
		for (Node<T> m : (dir == direction.FORWARD ? n.succ : n.pred)) {
			if (m.vistime == null) {
				visit(m, tree);
			}
		}
		n.finish(++time);
	}

	
	
}

class NodeComparator<T> implements Comparator<Node<T>> {
	
	public int compare(Node<T> o1, Node<T> o2) {
		return o2.fintime.compareTo(o1.fintime);
	}
}
