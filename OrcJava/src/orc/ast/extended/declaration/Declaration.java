package orc.ast.extended.declaration;

import orc.ast.extended.ASTNode;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A unit of syntax that encapsulates some declaration. Declarations affect the environment, 
 * for example by adding new bindings, but do not typically do computation on their own.
 * 
 * A declaration is scoped in the abstract syntax tree by a Declare object.
 * 
 * @author dkitchin
 *
 */
public abstract class Declaration implements ASTNode, Locatable { 
	protected SourceLocation location;
	public abstract orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) throws CompilationException;

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
