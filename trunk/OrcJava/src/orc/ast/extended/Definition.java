package orc.ast.extended;

import java.util.List;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Definitions are scoped in the abstract syntax tree through a Declare containing
 * a DefsDeclaration. 
 * 
 * @author dkitchin
 *
 */

public class Definition {

	public String name;
	public List<String> formals;
	public Expression body;
	
	public Definition(String name, List<String> formals, Expression body)
	{
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
	
}
