package orc.ast.extended.declaration;

import orc.ast.extended.Visitor;
import orc.ast.extended.type.ClassnameType;
import orc.ast.extended.type.SiteType;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
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

	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) {
		
		NamedVariable x = new NamedVariable(varname);
		Variable v = new Variable();
		
		orc.ast.sites.Site s = orc.ast.sites.Site.build(orc.ast.sites.Site.JAVA, classname);
		Argument a = new orc.ast.simple.argument.Site(s);
		
		return new WithLocation(
			new orc.ast.simple.expression.DeclareType(new ClassnameType(classname),
				varname,
				new orc.ast.simple.expression.Pruning(target.subvar(v,x), new orc.ast.simple.expression.Let(a), v)),
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

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
