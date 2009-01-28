package orc.error.compiletime.typing;

import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public abstract class InsufficientTypeInformationException extends TypeException {

	public InsufficientTypeInformationException(String message) {
		super(message);
	}

	public InsufficientTypeInformationException(String message, SourceLocation location) {
		super(message, location);
	}
}
