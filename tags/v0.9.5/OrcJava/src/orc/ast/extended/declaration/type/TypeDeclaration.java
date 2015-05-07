package orc.ast.extended.declaration.type;

import orc.ast.extended.declaration.Declaration;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.sites.Site;

/**
 * Declaration of an external type. The type is specified as a fully qualified Java class name.
 * The class must be a subclass of Type.
 * 
 * The declaration binds an instance of the class to the given type name.
 * 
 * @author dkitchin
 */

public class TypeDeclaration extends Declaration {

	public String varname;
	public String classname;
	
	public TypeDeclaration(String v, String c)
	{
		varname = v;
		classname = c;
	}
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
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

		
		
		/* External type declarations are currently ignored */
		return target;
	}

	public String toString() {
		return "type " + varname + " = " + classname;
	}
}
