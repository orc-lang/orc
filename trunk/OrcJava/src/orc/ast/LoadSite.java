/**
 * 
 */
package orc.ast;

import orc.runtime.Environment;
import orc.runtime.sites.Site;
import orc.runtime.values.Constant;

/**
 * @author dkitchin
 * 
 * AST node for a site loading directive:
 * 
 * site Foo = orc.lib.Foo
 *
 */
public class LoadSite implements EnvBinder {
	
	String var;
	String classname;
	
	public LoadSite(String var, String classname)
	{
		this.var = var;
		this.classname = classname;
	}
	
	public Environment bind(Environment env)
	{
		try
		{
			Class c = ClassLoader.getSystemClassLoader().loadClass(classname);
				
			// make sure that the class to be loaded is actually a subtype of Site
			if (Site.class.isAssignableFrom(c)) 
			{
				return new Environment(var, new Constant(c.newInstance()), env);
			}
			else	
			{	
				throw new Error("Class " + classname + " couldn't be loaded as a site becuase it is not a subclass of Site.");
			}
				
		}
		catch (Exception e) { throw new Error("Failed to load class " + classname + " as a site."); }
	}

}
