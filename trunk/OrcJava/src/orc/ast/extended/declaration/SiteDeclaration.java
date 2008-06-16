package orc.ast.extended.declaration;

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
		
		orc.ast.simple.arg.Argument a;
		orc.ast.simple.arg.NamedVar x;
		
		a = new orc.ast.simple.arg.Site(orc.ast.sites.Site.build(orc.ast.sites.Site.ORC, classname));
		x = new orc.ast.simple.arg.NamedVar(varname);
		return target.subst(a,x);
	}
}
