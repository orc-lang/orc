package orc.ast.extended.declaration;

import orc.runtime.sites.java.ClassProxy;

/**
 * Declaration of a class proxy. The class is given as a fully qualified Java class name.
 * It can be any Java class.
 * 
 * The declaration binds a proxy for this class to the given name. Calls to the proxy
 * behave as calls to the class's constructor.
 * 
 * @author dkitchin
 */

public class ClassDeclaration implements Declaration {

	public String varname;
	public String classname;
	
	public ClassDeclaration(String v, String c)
	{
		varname = v;
		classname = c;
	}

	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		Class<?> c;	
		try
		{
			c = ClassLoader.getSystemClassLoader().loadClass(classname);
		}
		catch (Exception e) { throw new Error("Failed to load class " + classname + " as a proxy."); }
		
		orc.ast.simple.arg.Argument a = new orc.ast.simple.arg.Site(new ClassProxy(c));
		orc.ast.simple.arg.NamedVar x = new orc.ast.simple.arg.NamedVar(varname);
		
		return target.subst(a,x);
	}
	
}
