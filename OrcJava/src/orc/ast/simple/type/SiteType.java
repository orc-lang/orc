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
 * A syntactic type corresponding to a Java class implementing a type.
 * 
 * In order to convert this to an actual type, the Java class must be
 * a subtype of orc.type.Type
 * 
 * @author dkitchin
 *
 */
public class SiteType extends Type {

	public String classname;
	
	public SiteType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public orc.type.Type convert(Env<String> env) {
		
		orc.type.Type t;
		Class<?> cls;
			
		try {
			cls = Class.forName(classname);
		}
		catch (ClassNotFoundException e) {
			throw new Error("Failed to load class " + classname + " as an Orc external type. Class not found.");
		}
			
		if (!orc.type.Type.class.isAssignableFrom(cls)) {
			throw new Error("Class " + cls + " cannot be used as an Orc external type because it is not a subtype of orc.type.Type."); 
		}
		
		try
		{
			t = (orc.type.Type)(cls.newInstance());
		} catch (InstantiationException e) {
			throw new Error("Failed to load class " + cls + " as an external type. Instantiation error.", e);
		} catch (IllegalAccessException e) {
			throw new Error("Failed to load class " + cls + " as an external type. Constructor is not accessible.");
		}
		
		return t;
	}
		
	public String toString() {		
		return classname;
	}	
	
}
