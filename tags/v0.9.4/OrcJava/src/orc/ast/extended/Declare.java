package orc.ast.extended;

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

	Declaration d;
	Expression e;
	
	public Declare(Declaration d, Expression e)
	{
		this.d = d;
		this.e = e;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		return d.bindto(e.simplify());
	}

	public String toString() {
		return d + "\n" + e;
	}
}
