package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.simple.TypeDecl;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.SiteType;
import orc.ast.simple.type.VariantTypeFormal;
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
	public List<VariantTypeFormal> formals;
	
	public TypeDeclaration(String varname, String classname,
			List<VariantTypeFormal> formals) {
		this.varname = varname;
		this.classname = classname;
		this.formals = formals;
	}

	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		return new TypeDecl(new SiteType(classname), varname, target);
	}

	public String toString() {
		return "type " + varname + " = " + classname;
	}
}
