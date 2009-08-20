package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.type.PolymorphicTypeAlias;
import orc.ast.extended.type.Type;
import orc.ast.simple.WithLocation;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.ast.simple.expression.TypeDecl;
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

	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) {
		
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

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
