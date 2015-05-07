package orc.ast.simple.arg;

import java.io.Serializable;
import java.util.Set;

import orc.env.Env;
import orc.runtime.values.Value;

/**
 * 
 * An AST class (distinct from Expression) which contains arguments to calls.
 * These arguments may either be variable names or constant values.
 * 
 * Note that in the call M(x,y), M is also technically an argument of the call.
 * This allows variables to be used in call position.
 * 
 * @author dkitchin
 *
 */

public abstract class Argument implements Serializable {
	
	public Argument subst(Argument newArg, NamedVar oldArg) {
		return equals(oldArg) ? newArg : this; 
	}
	public void addFree(Set<Var> freeset) {
		// Do nothing; overridden for arguments which may
		// be considered free in an expression
	}
	
	public abstract orc.ast.oil.arg.Arg convert(Env<Var> vars);
}