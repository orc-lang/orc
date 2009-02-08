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
 * A simple named type.
 * 
 * @author dkitchin
 *
 */
public class NamedType extends Type {

	public String name;
	
	public NamedType(String name) {
		this.name = name;
	}
	
	@Override
	public orc.type.Type convert(Env<String> env) throws TypeException {
		
		try {
			return new TypeVariable(env.search(name));
		} catch (SearchFailureException e) {
			throw new UnboundTypeException(name);
		}
	}
		
	public String toString() {		
		return name;
	}	
	
}
