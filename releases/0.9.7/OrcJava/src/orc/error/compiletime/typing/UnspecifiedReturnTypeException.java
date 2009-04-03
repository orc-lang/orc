package orc.error.compiletime.typing;

public class UnspecifiedReturnTypeException extends InsufficientTypeInformationException {

	public UnspecifiedReturnTypeException() {
		this("Could not perform type check due to missing return type; please add a return type annotation");
	}
	
	public UnspecifiedReturnTypeException(String message) {
		super(message);
	}

}
