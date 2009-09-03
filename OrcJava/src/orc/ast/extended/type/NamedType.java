package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;

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
	public orc.ast.simple.type.Type simplify() {
		return new orc.ast.simple.type.FreeTypeVariable(name);
	}
		
	public String toString() {		
		return name;
	}	
	
}
