package orc.error.runtime;



public class MethodTypeMismatchException extends RuntimeTypeException {

	public String methodName;
	
	public MethodTypeMismatchException(String methodName) {
		super("Argument types did not match any implementation for method '" + methodName + "'.");
		this.methodName = methodName;
	}

}
