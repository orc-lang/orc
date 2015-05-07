package orc.error.compiletime.typing;

/**
 * 
 * Exception raised 
 * 
 * @author dkitchin
 *
 */

public class MissingTypeException extends TypeException {

	public MissingTypeException() {
		super("Type checker failed: couldn't obtain sufficient type information from a service or value.");
	}
	
	public MissingTypeException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

}
