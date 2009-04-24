package orc.error.compiletime.typing;

/**
 * 
 * Exception raised 
 * 
 * @author dkitchin
 *
 */

public class MissingTypeException extends TypeException {
	
	public MissingTypeException(Throwable cause) {
		super(cause);
	}

	public MissingTypeException() {
		super("Type checker failed: couldn't obtain sufficient type information from a service or value.");
	}
	
	public MissingTypeException(String message) {
		super(message);
	}

	public MissingTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
