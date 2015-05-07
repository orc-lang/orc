package orc.ast.extended.declaration;

import java.util.List;

import orc.ast.extended.Expression;
import orc.ast.extended.pattern.Pattern;

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
	public List<Pattern> formals;
	public Expression body;
	
	public Definition(String name, List<Pattern> formals, Expression body)
	{
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
	
}
