package orc.error.runtime;


public class ArityMismatchException extends RuntimeTypeException {

	public int arityExpected;
	public int arityProvided;
	
	public ArityMismatchException(String message) {
		super(message);
	}

	public ArityMismatchException(String message, int arityExpected, int arityProvided) {
		super(message);
		this.arityExpected = arityExpected;
		this.arityProvided = arityProvided;
	}

	public ArityMismatchException(int arityExpected, int arityProvided) {
		super("Arity mismatch, expected " + arityExpected + " arguments, got " + arityProvided + " arguments.");
		this.arityExpected = arityExpected;
		this.arityProvided = arityProvided;
	}

}
