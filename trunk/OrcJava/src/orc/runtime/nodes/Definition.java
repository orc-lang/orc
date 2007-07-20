package orc.runtime.nodes;

import java.util.List;
import orc.ast.simple.arg.Var;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Groups of mutually recursive definitions are embedded in the execution graph by a Def.
 * 
 * @author dkitchin
 *
 */

public class Definition {

	public Var name;
	public List<Var> formals;
	public Node body;
	
	/**
	 * Note that the constructor takes a bound Var as a name parameter. This is because the
	 * binding of expression names occurs at the level of mutually recursive groups, not at
	 * the level of the individual definitions.
	 * 
	 * @param name
	 * @param formals
	 * @param body
	 */
	public Definition(Var name, List<Var> formals, Node body)
	{
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
	
}
