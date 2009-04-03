package orc.ast.extended.declaration;

import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.ClassType;
import orc.ast.simple.type.SiteType;
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

public class ClassDeclaration extends Declaration {

	public String varname;
	public String classname;
	
	public ClassDeclaration(String v, String c)
	{
		varname = v;
		classname = c;
	}

	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		NamedVar x = new NamedVar(varname);
		Var v = new Var();
		
		orc.ast.sites.Site s = orc.ast.sites.Site.build(orc.ast.sites.Site.JAVA, classname);
		Argument a = new orc.ast.simple.arg.Site(s);
		
		return new WithLocation(
			new orc.ast.simple.TypeDecl(new ClassType(classname),
				varname,
				new orc.ast.simple.Where(target.subvar(v,x), new orc.ast.simple.Let(a), v)),
			getSourceLocation());
		
		/*
		orc.ast.simple.arg.Argument a = new orc.ast.simple.arg.Site(orc.ast.sites.Site.build(orc.ast.sites.Site.JAVA, classname));
		orc.ast.simple.arg.NamedVar x = new orc.ast.simple.arg.NamedVar(varname);
		
		return target.subst(a,x);
		*/
	}
	public String toString() {
		return "class " + varname + " = " + classname;
	}	
}
