package orc.ast.oil;

import java.util.HashSet;
import java.util.Set;

import orc.ast.oil.arg.Var;
import orc.error.Debug;

/**
 * Base class for the portable (.oil, for Orc Intermediate Language) abstract syntax tree.
 * 
 * @author dkitchin
 *
 */

public abstract class Expr extends Debug {
	
	/**
	 * Compiles an oil syntax tree into an execution graph.
	 * Every node is compiled relative to an "output" node that represents
	 * the "rest of the program". Thus the tree of compiled nodes is created bottom up.
	 * @param output This is the node to which output (publications) will be directed.
	 * @return A new node.
	 */
	public abstract orc.runtime.nodes.Node compile(orc.runtime.nodes.Node output);
	
	
	/**
	 * Find the set of free variables in this expression. 
	 * 
	 * @return 	The set of free variables.
	 */
	public Set<Var> freeVars() {
		Set<Integer> indices = new HashSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Var> vars = new HashSet<Var>();
		for (Integer i : indices) {
			vars.add(new Var(i));
		}
		
		return vars;
	}
	
	/**
	 * If this expression has any indices which are >= depth,
	 * add (index - depth) to the index set accumulator. The depth 
	 * increases each time this method recurses through a binder.
	 * 
	 * The default implementation is to assume the expression
	 * has no free variables, and thus do nothing. Expressions
	 * which contain variables or subexpressions override this
	 * behavior.
	 * 
	 * @param vars   The index set accumulator.
	 * @param depth    The minimum index for a free variable. 
	 * @return
	 */
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}
	
	/**
	 * Marshal into a JAXB representation.
	 * @return
	 */
	public abstract orc.orchard.oil.Expression marshal();
}
