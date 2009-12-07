package orc.ast.extended.declaration.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.DeclareType;
import orc.ast.simple.expression.WithLocation;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.SiteType;
import orc.ast.simple.type.TypeVariable;
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
		
		orc.ast.simple.type.Type T = t.simplify();
		
		if (formals != null && formals.size() > 0) {
			List<orc.ast.simple.type.TypeVariable> newFormals = new LinkedList<orc.ast.simple.type.TypeVariable>();
			for (String formal : formals) {
				TypeVariable Y = new TypeVariable();
				FreeTypeVariable X = new FreeTypeVariable(formal);
				newFormals.add(Y);
				T = T.subvar(Y,X);
			}
			T = new orc.ast.simple.type.PolymorphicTypeAlias(T, newFormals);
		}
		
		orc.ast.simple.expression.Expression body = target;
		
		TypeVariable Y = new TypeVariable();
		FreeTypeVariable X = new FreeTypeVariable(typename);
		body = body.subvar(Y,X);
		body = new orc.ast.simple.expression.DeclareType(T, Y, body);
		body = new WithLocation(body, getSourceLocation());
		
		return body;
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
