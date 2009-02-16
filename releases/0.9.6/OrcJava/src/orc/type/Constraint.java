package orc.type;

import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;


/**
 * Upper and lower bound constraints on a type variable.
 * 
 * @author dkitchin
 */

public class Constraint {

	public Type lower = Type.BOT;
	public Type upper = Type.TOP;
	
	
	/* Add the constraint "T <: ..." */
	public void atLeast(Type T) throws TypeException {
		lower = T.join(lower);
		if (!lower.subtype(upper)) {
			throw new TypeException("Could not infer type arguments; overconstrained. " + lower + " </: " + upper);
		}
	}
	
	
	/* Add the constraint "... <: T" */
	public void atMost(Type T) throws TypeException {
		upper = T.meet(upper);
		if (!lower.subtype(upper)) {
			throw new TypeException("Could not infer type arguments; overconstrained. " + lower + " </: " + upper);
		}
	}
	
	/* Find the minimal type within these bounds under the given variance */
	public Type minimal(Variance v) throws TypeException {
		
		return v.minimum(lower, upper);
	}
	 
	public String toString() {
		return lower + " <: ... <: " + upper;
	}
}
