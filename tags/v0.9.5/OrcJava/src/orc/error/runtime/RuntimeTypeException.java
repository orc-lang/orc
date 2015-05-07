package orc.error.runtime;




/**
 * 
 * Superclass of all runtime type exceptions, including arity mismatches,
 * argument type mismatches, and attempts to call uncallable values.
 * 
 * @author dkitchin
 *
 */

public class RuntimeTypeException extends TokenException {

	public RuntimeTypeException(String message) {
		super(message);
	}

}
