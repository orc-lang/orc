package orc.error;

/**
 * 
 * Exceptions generated while executing a compiled graph.
 * 
 * @author dkitchin
 *
 */
public class ExecutionException extends OrcException {

	public ExecutionException(String message) {
		super(message);
	}

	public ExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

}
