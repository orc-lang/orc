package orc.ast.extended.declaration;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;
import orc.ast.simple.type.ClassnameType;
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
		
		orc.ast.sites.Site s = orc.ast.sites.Site.build(orc.ast.sites.Site.JAVA, classname);
		Argument a = new orc.ast.simple.argument.Site(s);
		orc.ast.simple.expression.Expression body = new orc.ast.simple.expression.Let(a);
		
		Variable v = new Variable();
		FreeVariable x = new FreeVariable(varname);
		body = new orc.ast.simple.expression.Pruning(target.subvar(v,x), body, v);
		 
		TypeVariable Y = new TypeVariable();
		FreeTypeVariable X = new FreeTypeVariable(varname);
		orc.ast.simple.type.Type T = new ClassnameType(classname);
		body = new orc.ast.simple.expression.DeclareType(T, Y, body);
		body = body.subvar(Y,X);
		
		body = new WithLocation(body, getSourceLocation());
		
		return body;
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
