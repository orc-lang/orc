package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeVariable;

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
		// TODO: Disallow this conversion; asserted types should be gone by this step.
		return type.convert(env);
	}
		
	public String toString() {		
		return type.toString() + " (asserted)";
	}	
	
}
