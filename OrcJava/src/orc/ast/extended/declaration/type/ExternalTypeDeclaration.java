package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.simple.TypeDecl;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.ExternalType;
import orc.ast.simple.type.VariantTypeFormal;
import orc.runtime.sites.Site;

/**
 * Declaration of an external type. The type is specified as a fully qualified Java class name.
 * The class must be a subclass of orc.type.Type.
 * 
 * @author dkitchin
 */

public class ExternalTypeDeclaration extends Declaration {

	public String typename;
	public String classname;
	public List<VariantTypeFormal> formals;
	
	public ExternalTypeDeclaration(String typename, String classname,
			List<VariantTypeFormal> formals) {
		this.typename = typename;
		this.classname = classname;
		this.formals = formals;
	}

	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {	
		// TODO: Allow type parameter checking of external types (formals are currently ignored)
		return new TypeDecl(new ExternalType(classname), typename, target);
	}

	public String toString() {
		return "type " + typename + " = " + classname;
	}
}
