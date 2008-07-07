package orc.error;

public class ParseError extends OrcException {
	public ParseError(String message, Throwable cause) {
		super(message, cause);
	}
	public ParseError(String message) {
		super(message);
	}
}
