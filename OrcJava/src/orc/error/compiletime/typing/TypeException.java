package orc.error.compiletime.typing;

import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public class TypeException extends CompilationException {

	public TypeException(String message) {
		super(message);
	}
	
	public TypeException(String message, SourceLocation location) {
		super(message);
		setSourceLocation(location);
	}

}
