package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
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
	public orc.ast.simple.Expression simplify() throws CompilationException {
				
		List<Argument> newargs = new LinkedList<Argument>();
		orc.ast.simple.Expression e = new orc.ast.simple.Let(newargs);
		
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
}
