package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

public class Quasidot extends Expression {

	public Expression target;
	public String field;
	
	public Quasidot(Dot d)
	{
		this.target = d.target;
		this.field = d.field;
	}
	
	@Override
	public orc.ast.simple.Expression simplify() {
		
		List<Expression> args = new LinkedList<Expression>();
		args.add(new Field(field));
		
		Expression e = new Call(target, args); 
			
		return e.simplify();
	}

}
