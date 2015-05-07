package orc.ast.simple.argument;

import java.util.Set;

import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.runtime.values.Value;


/**
 * Bound variables. Equivalence on these variables is physical (==) equality.
 * 
 * These occur in argument position. They also occur as fields in combinators
 * which bind variables.
 * 
 * @author dkitchin
 *
 */

public class Variable extends Argument {
	private static final long serialVersionUID = 1L;
	
	/* An optional string name to use for this variable in debugging contexts. */
	public String name = null;
	
	@Override
	public void addFree(Set<Variable> freeset) {
		freeset.add(this);
	}
	
	@Override
	public orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) {
		try {
			return new orc.ast.oil.expression.argument.Variable(vars.search(this));
		} catch (SearchFailureException e) {
			throw new OrcError(e);
		}
	}
	
	public String toString() {
		return (name != null ? name : "#" + hashCode());
	}
}