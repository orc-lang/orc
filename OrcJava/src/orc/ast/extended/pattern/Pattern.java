package orc.ast.extended.pattern;

import orc.ast.simple.arg.Var;


/**
 * 
 * Base interface for the abstract syntax of patterns.
 * 
 * Patterns exist only in the extended abstract syntax. They desugar into a series of operations
 * which terminate in variable bindings.
 * 
 * @author dkitchin
 * 
 */

public interface Pattern {

	public orc.ast.simple.Expression match(orc.ast.simple.Expression f);
	public orc.ast.simple.Expression bind(orc.ast.simple.Expression g, Var t);
	public boolean strict();
	
}
