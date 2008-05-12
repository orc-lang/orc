package orc.error;

public class OrcRuntimeTypeException extends OrcException {
	public OrcRuntimeTypeException(String string) {
		super(string);
	}
	public OrcRuntimeTypeException(String message, Throwable cause) {
		super(message, cause);
	}
	public OrcRuntimeTypeException(Throwable cause) {
		super(cause);
	}
}
