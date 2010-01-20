package orc.ast.extended.expression;

import orc.ast.extended.Visitor;
import orc.ast.extended.expression.Expression.Arg;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.pattern.VariablePattern;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.ast.extended.pattern.WildcardPattern;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.WithLocation;

/**
 * @author matsuoka 
 */
public class Throw extends Expression {
	public Expression exception;

	public Throw(Expression e){
		this.exception = e;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		return new WithLocation(new orc.ast.simple.expression.Throw(exception.simplify()), getSourceLocation());
	}
	
	public String toString() {
		return "(throw " + exception + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
