package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
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
	public orc.type.Type convert(Env<String> env) {
		
		try {
			return new TypeVariable(env.search(name));
		}
		catch (OrcError e) {
			System.out.println("WARNING: Type variable " + name + " is unbound; replacing with Top");
			return orc.type.Type.TOP;
		}
	}
		
	public String toString() {		
		return name;
	}	
	
}
