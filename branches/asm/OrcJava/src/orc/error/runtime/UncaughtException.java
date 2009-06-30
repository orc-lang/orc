package orc.error.runtime;

public class UncaughtException extends TokenException {

	public UncaughtException(String message) {
		super(message);
	}

	public UncaughtException(String message, Throwable cause) {
		super(message, cause);
	}

}