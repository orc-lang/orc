package orc.ast.extended.declaration;

import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.sites.Site;

/**
 * Declaration of a site. The site is specificed as a fully qualified Java class name.
 * The class must be a subclass of Site.
 * 
 * The declaration binds an instance of the class to the given name.
 * 
 * @author dkitchin
 */

public class SiteDeclaration extends Declaration {

	public String varname;
	public String classname;
	
	public SiteDeclaration(String v, String c)
	{
		varname = v;
		classname = c;
	}
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		
		NamedVar x = new NamedVar(varname);
		Var v = new Var();
		
		orc.ast.sites.Site s = orc.ast.sites.Site.build(orc.ast.sites.Site.ORC, classname);
		Argument a = new orc.ast.simple.arg.Site(s);
		
		return new WithLocation(
			new orc.ast.simple.Where(target.subst(v,x), new orc.ast.simple.Let(a), v),
			getSourceLocation());
		
		/*
		orc.ast.simple.arg.Argument a;
		orc.ast.simple.arg.NamedVar x;
		
		a = new orc.ast.simple.arg.Site(orc.ast.sites.Site.build(orc.ast.sites.Site.ORC, classname));
		x = new orc.ast.simple.arg.NamedVar(varname);
		return target.subst(a,x);
		*/
	}
	public String toString() {
		return "site " + varname + " = " + classname;
	}
}
