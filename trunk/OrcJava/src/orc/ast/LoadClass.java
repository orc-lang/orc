/**
 * 
 */
package orc.ast;

import orc.runtime.Environment;
import orc.runtime.sites.java.ClassProxy;
import orc.runtime.values.Constant;

/**
 * @author dkitchin
 *
 * AST node for a class loading directive:
 * 
 * class Baz = orc.lib.Baz
 *
 */
public class LoadClass implements EnvBinder {

	String var;
	String classname;
	
	public LoadClass(String var, String classname)
	{
		this.var = var;
		this.classname = classname;
	}
	
	
	public Environment bind(Environment env) {
		try
		{
			Class c = ClassLoader.getSystemClassLoader().loadClass(classname);
			return new Environment(var, new Constant(new ClassProxy(c)), env);
		}
		catch (Exception e) { throw new Error("Failed to load class " + classname + " as a site."); }	
	}

}
