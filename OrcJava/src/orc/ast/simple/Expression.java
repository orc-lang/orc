
package orc.ast.simple;

import java.util.Map;
import java.util.Set;

import orc.ast.extended.Call;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.UnboundVariableException;
import orc.runtime.nodes.Node;
import orc.type.Type;

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
	 * 
	 * Note that the typevars environment is a content-addressable environment
	 * of strings. Type vars are represented as strings for simplicity, because
	 * unlike regular vars, there are not yet any compiler steps which generate 
	 * temporary type variables.
	 * 
	 * @param vars	The vars environment, used in content addressable mode to 
	 * 				find the appropriate deBruijn index of a var.
	 * @param typevars The type vars environment, used in content addressable
	 * 				   mode to find the appropriate deBruijn index of a type var.
	 * @return A new node.
	 */
	public abstract orc.ast.oil.Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException;
	
	
	/**
	 * Performs the substitution [a/x], replacing occurrences of the free variable x
	 * with the new argument a (which could be any argument, including another variable).
	 * 
	 * @param a The replacing variable or value
	 * @param x The free variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the expression with the substitution performed
	 */
	public abstract Expression subst(Argument a, NamedVar x);
	
	
	/**
	 * 
	 * Performs the substitution [v/x], replacing occurrences of the free variable x
	 * with the nameless variable v. Additionally, attach the name of x as documentation
	 * on the variable v so that it can be used later for debugging or other purposes.
	 * 
	 * This method delegates to the subst method after attaching the name. 
	 * 
	 * @param v The replacing variable
	 * @param x The free variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the expression with the substitution performed
	 */
	public Expression subvar(Var v, NamedVar x) {
		v.giveName(x.key);
		return subst(v,x);
	}
	
	/**
	 * Perform a set of substitutions defined by a map.
	 * For each x |-> a in the map, the substitution [a/x] occurs.
	 * 
	 * If a is a nameless variable v, the name of the corresponding
	 * x will be attached to it.
	 * 
	 * @param m
	 */
	public Expression suball(Map<NamedVar, ? extends Argument> m)
	{
		Expression result = this;
		
		for (NamedVar x : m.keySet())
		{
			Argument a = m.get(x);
			if (a instanceof Var) {
				((Var)a).giveName(x.key);
			}
			result = result.subst(a,x);
		}
		
		return result;
	}
	
	/**
	 * Find the set of all unbound Vars (note: not FreeVars) in this expression.
	 */
	public abstract Set<Var> vars();
}
