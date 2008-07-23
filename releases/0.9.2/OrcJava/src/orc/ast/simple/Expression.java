package orc.ast.simple;

import java.util.Map;
import java.util.Set;

import orc.ast.extended.Call;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.runtime.nodes.Node;

/**
 * Base class for the simplified abstract syntax tree.
 * 
 * @author dkitchin, wcook
 *
 */

public abstract class Expression {
	
	/**
	 * Converts abstract syntax tree into a serializable form, used to generate
	 * portable .oil (Orc Intermediate Language) files.
	 * @param vars	The vars environment, used in content addressable mode to 
	 * 				find the appropriate deBruijn index of a var.
	 * @return A new node.
	 */
	public abstract orc.ast.oil.Expr convert(Env<Var> vars);
	
	
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
	 */
	public abstract Set<Var> vars();

	
	/**
	 * Intermediate step to allow a one-step compile from the simple AST.
	 * Obviously this is only useful if the compilation and execution
	 * environments are colocated.
	 */
	public Node compile(Node output) {
		return this.convert(new Env<Var>()).compile(output);
	}

}
