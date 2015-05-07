package orc.ast.extended;

import orc.ast.extended.declaration.Declaration;

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
	public orc.ast.simple.Expression simplify() {
		
		return d.bindto(e.simplify());
	}

}
