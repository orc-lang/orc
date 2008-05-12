package orc.error;

public class OrcException extends Exception {
	public OrcException(String string) {
		super(string);
	}
	public OrcException(String message, Throwable cause) {
		super(message, cause);
	}
	public OrcException(Throwable cause) {
		super(cause);
	}	
}
