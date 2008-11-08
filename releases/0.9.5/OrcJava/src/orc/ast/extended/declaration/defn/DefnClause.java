package orc.ast.extended.declaration.defn;

import java.util.List;
import java.util.Map;

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

public class DefnClause extends Defn {

	public List<Pattern> formals;
	public Expression body;
	
	public DefnClause(String name, List<Pattern> formals, Expression body)
	{
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
	public String toString() {
		return "def " + name + " (" + Expression.join(formals, ", ") + ") = " + body;
	}
	
	public void extend(AggregateDefn adef) {
		adef.addClause(new Clause(formals, body));
	}	
}
