package orc.error.runtime;



public class ArgumentTypeMismatchException extends RuntimeTypeException {

	int position;
	String expectedType;
	String providedType;
	
	public ArgumentTypeMismatchException(String message) {
		super(message);
	}

	public ArgumentTypeMismatchException(int position,
			String expectedType, String providedType) {
		super("Expected type " + expectedType + " for argument " + position + ", got " + providedType + " instead");
		this.position = position;
		this.expectedType = expectedType;
		this.providedType = providedType;
	}

	public ArgumentTypeMismatchException(String message, int position,
			String expectedType, String providedType) {
		super(message);
		this.position = position;
		this.expectedType = expectedType;
		this.providedType = providedType;
	}

	public ArgumentTypeMismatchException(ClassCastException e) {
		super(e.toString());
	}

}
