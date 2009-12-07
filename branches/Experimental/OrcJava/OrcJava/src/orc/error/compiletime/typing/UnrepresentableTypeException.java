package orc.error.compiletime.typing;

import orc.type.Type;

/**
 * Thrown when trying to marshal a generated type which cannot
 * be represented syntactically.
 * @author quark
 */
public class UnrepresentableTypeException extends TypeException {
	public UnrepresentableTypeException(Type type) {
		super(type.toString() + " has no concrete syntax.");
	}
	public UnrepresentableTypeException() {
		super("Unrepresentable type");
	}
}
