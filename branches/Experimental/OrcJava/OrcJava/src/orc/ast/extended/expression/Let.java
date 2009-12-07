package orc.ast.extended.expression;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.expression.WithLocation;
import orc.error.compiletime.CompilationException;

public class Let extends Expression {

	public List<Expression> args;
	
	// unary constructor
	public Let(Expression arg)
	{
		this.args = new LinkedList<Expression>();
		this.args.add(arg);
	}
	
	public Let(List<Expression> args)
	{
		this.args = args;
	}

	public Let() {
		this.args = new LinkedList<Expression>();
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
				
		List<Argument> newargs = new LinkedList<Argument>();
		orc.ast.simple.expression.Expression e = new orc.ast.simple.expression.Let(newargs);
		
		for (Expression r : args)
		{
			Arg a = r.argify();
			newargs.add(a.asArg());
			e = a.bind(e);
		}
		
		return new WithLocation(e, getSourceLocation());
	}

	public String toString() {
		return "(" + join(args, ", ") + ")";
	}	

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
