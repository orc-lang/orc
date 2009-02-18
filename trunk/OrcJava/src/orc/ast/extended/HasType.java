package orc.ast.extended;

import orc.ast.simple.WithLocation;
import orc.ast.simple.type.AssertedType;
import orc.ast.simple.type.Type;
import orc.error.compiletime.CompilationException;

public class HasType extends Expression {

	public Expression body;
	public Type type;

	public HasType(Expression body, Type type)
	{
		this.body = body;
		this.type = type;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		boolean checkable = true;
		
		/* If this is an asserted type, it is not checkable */
		if (type instanceof AssertedType) {
			AssertedType atype = (AssertedType)type;
			type = atype.type;
			checkable = false;
		}
		
		return new WithLocation(
				new orc.ast.simple.HasType(body.simplify(), type, checkable),
				getSourceLocation());
	}
	
	public String toString() {
		return "(" + body + " :: " + type + ")";
	}
}
