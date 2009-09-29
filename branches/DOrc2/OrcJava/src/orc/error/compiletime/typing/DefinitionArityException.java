package orc.error.compiletime.typing;

public class DefinitionArityException extends TypeException {

	public Integer arityFromType;
	public Integer arityFromSyntax;
	
	public DefinitionArityException(String message) {
		super(message);
	}

	public DefinitionArityException(int arityFromType, int arityFromSyntax) {
		super("Definition should have " + arityFromType + " arguments according to its type, observed " + arityFromSyntax + " arguments instead.");
		this.arityFromType = arityFromType;
		this.arityFromSyntax = arityFromSyntax;
	}

	
}
