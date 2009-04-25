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
 * A syntactic type which refers to a Java class (which we will treat as a type).
 * @author quark, dkitchin
 */
public class ClassnameType extends Type {

	public String classname;
	
	public ClassnameType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.type.Type convert(Env<String> env) {
		return new orc.type.ClassnameType(classname);
	}
		
	public String toString() {		
		return classname;
	}	
	
}
