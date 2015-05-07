package orc.error.compiletime.typing;

import orc.type.Type;

/**
 * 
 * Exception raised when the typechecker finds an uncallable
 * value in call position.
 * 
 * @author dkitchin
 *
 */

public class UncallableTypeException extends TypeException {

	Type t;
	
	public UncallableTypeException(Type t) {
		super("Type " + t + " cannot be called as a service or function.");
		this.t = t;
	}
	
}
