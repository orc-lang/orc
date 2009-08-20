package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.type.SiteType;
import orc.ast.simple.WithLocation;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.ast.simple.expression.TypeDecl;
import orc.runtime.sites.Site;

/**
 * Declaration of an external type. The type is specified as a fully qualified Java class name.
 * The class must be a subclass of orc.type.Type.
 * 
 * The declaration binds an instance of the class to the given type name.
 * 
 * @author dkitchin
 */

public class TypeDeclaration extends Declaration {

	public String varname;
	public String classname;
	
	public TypeDeclaration(String varname, String classname) {
		this.varname = varname;
		this.classname = classname;
	}

	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) {
		
		return new TypeDecl(new SiteType(classname), varname, target);
	}

	public String toString() {
		return "type " + varname + " = " + classname;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
