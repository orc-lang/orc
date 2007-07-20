package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

public class Dot extends Expression {

	public Expression target;
	public String field;
	
	public Dot(Expression target, String field)
	{
		this.target = target;
		this.field = field;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		List<Expression> args = new LinkedList<Expression>();
		args.add(new Field(field));
		
		Expression e = new Call(target, args); 
		
		e = new Call(e, new LinkedList<Expression>());
			
		return e.simplify();
	}

}
