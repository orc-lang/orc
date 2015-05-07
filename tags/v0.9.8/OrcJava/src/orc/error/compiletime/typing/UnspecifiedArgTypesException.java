package orc.error.compiletime.typing;

public class UnspecifiedArgTypesException extends InsufficientTypeInformationException {

	public UnspecifiedArgTypesException() {
		this("Could not perform type check due to missing argument types; please add argument type annotations");
	}
	
	public UnspecifiedArgTypesException(String message) {
		super(message);
	}

}
