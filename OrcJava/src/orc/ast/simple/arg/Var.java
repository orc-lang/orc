package orc.ast.simple.arg;

import java.util.Set;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
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

public class Var extends Argument {
	private static final long serialVersionUID = 1L;
	@Override
	public void addFree(Set<Var> freeset) {
		freeset.add(this);
	}
	
	@Override
	public Arg convert(Env<Var> vars) {
		return new orc.ast.oil.arg.Var(vars.search(this));
	}
}