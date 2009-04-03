package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.simple.TypeDecl;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.VariantTypeFormal;
import orc.runtime.sites.Site;

/**
 * Creating a new alias for an existing type.
 * 
 * @author dkitchin
 */

public class TypeAliasDeclaration extends Declaration {

	public String typename;
	public Type t;
	public List<VariantTypeFormal> formals;
	
	public TypeAliasDeclaration(String typename, Type t,
			List<VariantTypeFormal> formals) {
		this.typename = typename;
		this.t = t;
		this.formals = formals;
	}

	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		// TODO: Allow polymorphic type aliasing (formals are currently ignored)
		return new TypeDecl(t, typename, target);
	}

	public String toString() {
		return "type " + typename + " = " + t;
	}
}
