package orc.ast.simple.type;

import orc.env.Env;

public class ExternalType extends Type {

	public String classname;
	
	public ExternalType(String classname) {
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

}
