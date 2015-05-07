package orc.error.compiletime.typing;

import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;


public class UnboundTypeException extends TypeException {
	
	public UnboundTypeException(String typename) {
		super("Type " + typename + " is undefined");
	}
}
