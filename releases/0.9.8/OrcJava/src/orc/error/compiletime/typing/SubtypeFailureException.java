package orc.error.compiletime.typing;

import orc.type.Type;

public class SubtypeFailureException extends TypeException {

	Type S;
	Type T;
	
	public SubtypeFailureException(Type S, Type T) {
		super("Expected type " + T + " or some subtype, found type " + S + " instead.");
		this.S = S;
		this.T = T;
	}

}
