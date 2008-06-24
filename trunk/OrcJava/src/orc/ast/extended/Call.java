package orc.ast.extended;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.arg.Argument;
import orc.error.Locatable;
import orc.error.SourceLocation;

public class Call extends Expression implements Locatable {

	public Expression caller;
	public List<Expression> args;
	public SourceLocation location;
		
	public Call(Expression caller, List<Expression> args)
	{
		this.caller = caller;
		this.args = args;
	}
	
	public Call(Expression caller, Expression arg)
	{
		this.caller = caller;
		this.args = new ArrayList<Expression>();
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
		this.caller = new Name(s);
		this.args = args;
	}
	public Call(String s, Expression arg)
	{
		this.caller = new Name(s);
		this.args = new ArrayList<Expression>();
		this.args.add(arg);
	}
	public Call(String s)
	{
		this.caller = new Name(s);
		this.args = new ArrayList<Expression>();
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		List<Argument> newargs = new LinkedList<Argument>();
		Arg newcaller = caller.argify();
		orc.ast.simple.Expression e = new orc.ast.simple.Call(newcaller.asArg(), newargs);
				
		e = newcaller.bind(e);
		
		for (Expression r : args)
		{
			Arg a = r.argify();
			newargs.add(a.asArg());
			e = a.bind(e);
		}
		
		return e;
	}

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}

}
