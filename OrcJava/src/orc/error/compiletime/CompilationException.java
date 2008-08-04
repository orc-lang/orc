package orc.error.compiletime;

import orc.error.OrcException;


/**
 * 
 * Exceptions generated during Orc compilation from source to
 * portable compiled representations.
 * 
 * @author dkitchin
 *
 */
public class CompilationException extends OrcException {

	public CompilationException(String message) {
		super(message);
	}

	public CompilationException(String message, Throwable cause) {
		super(message, cause);
	}

}
