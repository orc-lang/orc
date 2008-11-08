package orc.error.compiletime;

import orc.error.SourceLocation;


public class UnboundVariableException extends CompilationException {
	public UnboundVariableException(String key, SourceLocation location) {
		this("Variable " + key + " is unbound at " + location);
	}
	public UnboundVariableException(String message) {
		super(message);
	}
}
