package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A declaration together with its scope in the AST.
 * 
 * @author dkitchin
 *
 */

public class Declare extends Expression {

	public Declaration d;
	public Expression e;
	
	public Declare(Declaration d, Expression e)
	{
		this.d = d;
		this.e = e;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		return d.bindto(e.simplify());
	}

	public String toString() {
		return d + "\n" + e;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
