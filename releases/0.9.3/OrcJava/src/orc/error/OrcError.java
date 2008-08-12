package orc.error;

/**
 * 
 * Error conditions that should never occur. The occurrence of such
 * an error at runtime indicates the violation of some language
 * invariant. In general this can substitute for AssertionError.
 * 
 * @author dkitchin
 *
 */

public class OrcError extends Error {

	public OrcError(String message) {
		super(message);
	}

	public OrcError(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public OrcError(Throwable arg0) {
		super(arg0);
	}

}
