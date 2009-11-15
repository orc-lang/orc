package orc.error.runtime;


public class InsufficientArgsException extends RuntimeTypeException {

	public int missingArg;
	public int arityProvided;
	
	public InsufficientArgsException(String message) {
		super(message);
	}

	public InsufficientArgsException(String message, int missingArg, int arityProvided) {
		super(message);
		this.missingArg = missingArg;
		this.arityProvided = arityProvided;
	}

	public InsufficientArgsException(int missingArg, int arityProvided) {
		super("Arity mismatch, could not find argument " + missingArg + ", only got " + arityProvided + " arguments.");
		this.missingArg = missingArg;
		this.arityProvided = arityProvided;
	}

}
