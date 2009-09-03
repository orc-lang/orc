package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.simple.expression.WithLocation;

public class Name extends Expression {

	public String name;
	
	public Name(String name)
	{
		this.name = name;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() {
		orc.ast.simple.argument.Argument var = new orc.ast.simple.argument.FreeVariable(name);
		var.setSourceLocation(getSourceLocation());
		return new WithLocation(new orc.ast.simple.expression.Let(var),
				getSourceLocation());
	}

	public Arg argify() {
		orc.ast.simple.argument.Argument var = new orc.ast.simple.argument.FreeVariable(name);
		var.setSourceLocation(getSourceLocation());
		return new simpleArg(var);
	}
	
	public String toString() {
		return name;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
