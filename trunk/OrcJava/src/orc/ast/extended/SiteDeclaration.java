package orc.ast.extended;

import orc.runtime.sites.Site;

/**
 * Declaration of a site. The site is specificed as a fully qualified Java class name.
 * The class must be a subclass of Site.
 * 
 * The declaration binds an instance of the class to the given name.
 * 
 * @author dkitchin
 */

public class SiteDeclaration implements Declaration {

	public String varname;
	public String classname;
	
	public SiteDeclaration(String v, String c)
	{
		varname = v;
		classname = c;
	}
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		Class<?> c;
		orc.ast.simple.arg.Argument a;
		orc.ast.simple.arg.NamedVar x;
		
		try
		{
			c = ClassLoader.getSystemClassLoader().loadClass(classname);
			
			if (Site.class.isAssignableFrom(c)) 
			{
				a = new orc.ast.simple.arg.Site((orc.runtime.sites.Site)c.newInstance());
			}
			else
			{ 
				throw new Error("Class " + classname + " cannot be used as a site. It is not a subtype of Site."); 
			}
		}
		catch (Exception e) { throw new Error("Failed to load class " + classname + " as a site."); }
		
		x = new orc.ast.simple.arg.NamedVar(varname);
		return target.subst(a,x);
	}
}
