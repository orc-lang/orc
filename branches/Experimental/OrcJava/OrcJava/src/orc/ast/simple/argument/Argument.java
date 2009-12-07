package orc.ast.simple.argument;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.expression.Expression;
import orc.ast.simple.type.Type;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.UnboundVariableException;
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

public abstract class Argument implements Serializable, Locatable {
	private SourceLocation location;
	
	public Argument subst(Argument newArg, FreeVariable oldArg) {
		return equals(oldArg) ? newArg : this; 
	}
	
	/**
	 * Convenience method, to apply a substitution to a list of arguments.
	 */
	public static List<Argument> substAll(List<Argument> args, Argument a, FreeVariable x) {
		List<Argument> newargs = new LinkedList<Argument>();
		for (Argument arg : args) {
			newargs.add(arg.subst(a, x));
		}
		return newargs;
	}
	
	public void addFree(Set<Variable> freeset) {
		// Do nothing; overridden for arguments which may
		// be considered free in an expression
	}
	
	/**
	 * Convert to DeBruijn index.
	 */
	public abstract orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) throws UnboundVariableException;
	
	public static List<orc.ast.oil.expression.argument.Argument> convertAll(List<Argument> as, Env<Variable> vars) throws UnboundVariableException {
		
		List<orc.ast.oil.expression.argument.Argument> newas = new LinkedList<orc.ast.oil.expression.argument.Argument>();
		
		for (Argument a : as) {
			newas.add(a.convert(vars));
		}
		
		return newas;
	}
	
	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}