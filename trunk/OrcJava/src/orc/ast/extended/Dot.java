package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * A dot expression (e.g "C.get"). Dot expressions can be chained together, "C.get.put(4)".
 * 
 * The parser distinguishes two cases where dot is used:
 * 
 * (Quasidot) The dot call has an explicit argument list:
 * 
 *     C.put(4)
 *     
 * This is interpreted as a chain of calls:
 * 
 *     C(`put`)(4)
 *     
 * where `put` is a special Field object.
 * 
 * (Dot) The dot call has no argument list:
 *    
 *     C.get
 * 
 * This is interpreted as a chain of calls, the second of which has no arguments:
 * 
 *     C(`get`)()
 * 
 * where `get` is a special Field object.
 * 
 * @author dkitchin
 *
 */

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
