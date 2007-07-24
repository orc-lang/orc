package orc.ast.extended;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.arg.Argument;

public class Call extends Expression {

	public Expression caller;
	public List<Expression> args;
	
	public Call(Expression caller, List<Expression> args)
	{
		this.caller = caller;
		this.args = args;
	}
	
	/* Alternate constructor for sites with string names, such as ops */
	public Call(String s, List<Expression> args)
	{
		this.caller = new Name(s);
		this.args = args;
	}
	
	/* Special case alternate constructor for unary ops */
	public Call(String s, Expression arg)
	{
		this.caller = new Name(s);
		this.args = new ArrayList<Expression>();
		this.args.add(arg);
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
				
		if (caller instanceof Dot) { caller = new Quasidot((Dot)caller); }
		
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

}
