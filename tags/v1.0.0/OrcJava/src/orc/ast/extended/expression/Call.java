package orc.ast.extended.expression;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.expression.WithLocation;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

public class Call extends Expression {

	public Expression caller;
	public List<Expression> args;
	public List<Type> typeArgs = null;
	
	public Call(Expression caller, List<Expression> args, List<Type> typeArgs)
	{
		this.caller = caller;
		this.args = args;
		this.typeArgs = typeArgs;
	}
	
	public Call(Expression caller, List<Expression> args)
	{
		this.caller = caller;
		this.args = args;
	}
	
	public Call(Expression caller, Expression arg)
	{
		this(caller);
		this.args.add(arg);
	}
	public Call(Expression caller)
	{
		this.caller = caller;
		this.args = new ArrayList<Expression>();
	}
	
	/* Alternate constructors for sites with string names, such as ops */
	public Call(String s, List<Expression> args)
	{
		this(new Name(s), args);
	}
	public Call(String s, Expression left, Expression right)
	{
		this(s);
		this.args.add(left);
		this.args.add(right);
	}
	public Call(String s, Expression arg)
	{
		this(s);
		this.args.add(arg);
	}
	public Call(String s)
	{
		this.caller = new Name(s);
		this.args = new ArrayList<Expression>();
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		List<Argument> newargs = new LinkedList<Argument>();
		Arg newcaller = caller.argify();
		
		List<orc.ast.simple.type.Type> newTypeArgs = null;
		if (typeArgs != null) {
			newTypeArgs = new LinkedList<orc.ast.simple.type.Type>();
			for (Type t : typeArgs) {
				newTypeArgs.add(t.simplify());
			}
		}
		
		orc.ast.simple.expression.Expression e = new orc.ast.simple.expression.Call(newcaller.asArg(), newargs, newTypeArgs);
		e = newcaller.bind(e);
		
		for (Expression r : args)
		{
			Arg a = r.argify();
			newargs.add(a.asArg());
			e = a.bind(e);
		}
		
		SourceLocation location = getSourceLocation();
		return location != null
			? new WithLocation(e, getSourceLocation())
			: e;
	}

	public String toString() {
		return caller.toString() + "(" + join(args, ", ") + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
