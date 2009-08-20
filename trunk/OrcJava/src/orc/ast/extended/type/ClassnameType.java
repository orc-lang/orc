package orc.ast.extended.type;

import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.java.ClassTycon;

/**
 * A syntactic type which refers to a Java class (which we will treat as an Orc type).
 * @author quark, dkitchin
 */
public class ClassnameType extends Type {

	public String classname;
	
	public ClassnameType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.type.Type convert(Env<String> env) throws TypeException {
		return new orc.type.ClassnameType(classname);
		// FIXME: the following breaks conversion between orc.ast.oil and orc.ast.oil.xml.
		// See comments in issue 26.
		/*
		
		Class cls;
		
		try
		{
			cls = Class.forName(classname);
		} catch (ClassNotFoundException e) {
			throw new TypeException("Failed to load class " + classname + " as a type.");
		}
		
		return orc.type.Type.fromJavaClass(cls);
		*/

	}
		
	public String toString() {		
		return classname;
	}	
	
}
