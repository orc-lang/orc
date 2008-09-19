package orc.error.runtime;


/**
 * 
 * Exception raised when an uncallable value occurs in call position.
 * 
 * @author dkitchin
 *
 */
public class UncallableValueException extends RuntimeTypeException {

	public UncallableValueException(String message) {
		super(message);
	}

}
