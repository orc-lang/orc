package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

public class Tuple extends Expression {

	public List<Expression> items;
	
	public Tuple(List<Expression> items)
	{
		this.items = items;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		List<orc.ast.simple.arg.Argument> args = new LinkedList<orc.ast.simple.arg.Argument>();
		orc.ast.simple.Expression e = new orc.ast.simple.Let(args);
		
		for (Expression i : items)
		{
			Arg a = i.argify();
			args.add(a.asArg());
			e = a.bind(e);
		}
		
		return e;
	}

}