package orc.error.compiletime.typing;

public class TypeArityException extends TypeException {

	public Integer arityExpected;
	public Integer arityReceived;
	
	public TypeArityException(String message) {
		super(message);
	}

	public TypeArityException(int arityExpected, int arityReceived) {
		super("Expected " + arityExpected + " arguments to type instantiation, got " + arityReceived + " arguments instead.");
		this.arityExpected = arityExpected;
		this.arityReceived = arityReceived;
	}

	
}
