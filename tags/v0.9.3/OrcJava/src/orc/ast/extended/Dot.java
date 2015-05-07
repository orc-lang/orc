package orc.ast.extended;

import orc.error.compiletime.CompilationException;

/**
 * 
 * A dot expression (e.g "C.put(4)"). 
 *     
 * This is interpreted as a chain of calls:
 * 
 *     C(`put`)(4)
 *     
 * where `put` is a special Field object.
 * 
 * @author dkitchin
 *
 */

public class Dot extends Expression {

	public Expression target;
	public String field;
	
	public Dot(Expression target, String field)
	{
		this.target = target;
		this.field = field;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		Expression e = new Call(target, new Field(field));
		e.setSourceLocation(getSourceLocation());
		return e.simplify();
	}

	public String toString() {
		return target + "." + field;
	}
}
