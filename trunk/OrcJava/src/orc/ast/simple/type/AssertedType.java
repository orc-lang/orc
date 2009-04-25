package orc.ast.simple.type;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * A type which is in some asserted position.
 * 
 * @author dkitchin
 *
 */
public class AssertedType extends Type {

	public Type type;
	
	public AssertedType(Type type) {
		this.type = type;
	}
	
	@Override
	public orc.type.Type convert(Env<String> env) throws TypeException {
		// asserted types should be gone by this step.
		throw new AssertionError("Unexpected AssertedType");
	}
		
	public String toString() {		
		return type.toString() + " (asserted)";
	}	
	
}
