package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.declaration.Declaration;
import orc.ast.simple.TypeDecl;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.PolymorphicTypeAlias;
import orc.ast.simple.type.Type;
import orc.runtime.sites.Site;

/**
 * Creating a new alias for an existing type.
 * 
 * @author dkitchin
 */

public class TypeAliasDeclaration extends Declaration {

	public String typename;
	public Type t;
	public List<String> formals;
	
	public TypeAliasDeclaration(String typename, Type t,
			List<String> formals) {
		this.typename = typename;
		this.t = t;
		this.formals = formals;
	}

	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		if (formals != null && formals.size() > 0) {
			return new TypeDecl(new PolymorphicTypeAlias(t, formals), typename, target);
		}
		else {
			return new TypeDecl(t, typename, target);
		}
	}

	public String toString() {
		return "type " + typename + " = " + t;
	}
}
