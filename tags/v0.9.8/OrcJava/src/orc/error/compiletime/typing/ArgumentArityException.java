package orc.error.compiletime.typing;

public class ArgumentArityException extends TypeException {

	public Integer arityExpected;
	public Integer arityReceived;
	
	public ArgumentArityException(String message) {
		super(message);
	}

	public ArgumentArityException(int arityExpected, int arityReceived) {
		super("Expected " + arityExpected + " arguments to call, got " + arityReceived + " arguments instead.");
		this.arityExpected = arityExpected;
		this.arityReceived = arityReceived;
	}

	
}
