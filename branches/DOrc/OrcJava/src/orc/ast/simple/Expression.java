package orc.ast.simple;

import java.util.Map;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.error.Debug;

/**
 * Base class for the simplified abstract syntax tree.
 * 
 * @author dkitchin, wcook
 *
 */

public abstract class Expression extends Debug {

	/**
	 * Compiles abstract syntax tree into execution nodes.
	 * Every node is compiled relative to an "output" node that represents
	 * the "rest of the program". Thus the tree of compiled nodes is created bottom up.
	 * @param output This is the node to which output (publications) will be directed.
	 * @return A new node.
	 */
	public abstract orc.runtime.nodes.Node compile(orc.runtime.nodes.Node output);
	
	
	/**
	 * Performs the substitution [a/x], replacing occurrences of the free variable x
	 * with the new argument a (which could be any argument, including another variable).
	 * 
	 * @param a The replacing variable or value
	 * @param x The free variable whose occurrences will be replaced
	 */
	public abstract Expression subst(Argument a, NamedVar x);
	
	
	/**
	 * Perform a set of substitutions defined by a map.
	 * For each x |-> a in the map, the substitution [a/x] occurs.
	 * 
	 * @param m
	 */
	public Expression suball(Map<NamedVar, ? extends Argument> m)
	{
		Expression result = this;
		
		for (NamedVar x : m.keySet())
		{
			result = result.subst(m.get(x),x);
		}
		
		return result;
	}
	
	/**
	 * Find the set of all unbound Vars (note: not FreeVars) in this expression.
	 * @return
	 */
	public abstract Set<Var> vars();
}
