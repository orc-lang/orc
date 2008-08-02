package orc.error;

public class UnboundVariableException extends CompilationException {
	public UnboundVariableException(String key, SourceLocation location) {
		this("Variable " + key + " is unbound at " + location);
	}
	public UnboundVariableException(String message) {
		super(message);
	}
}
