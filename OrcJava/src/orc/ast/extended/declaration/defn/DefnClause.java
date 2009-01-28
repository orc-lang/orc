package orc.ast.extended.declaration.defn;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.extended.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.simple.type.Type;

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
	public Type resultType;
	
	public DefnClause(String name, List<Pattern> formals, Expression body, Type resultType)
	{
		this.name = name;	/* name is "" when used for anonymous functions */
		this.formals = formals;
		this.body = body;
		this.resultType = resultType;
	}
	public String toString() {
		String prefix = name.equals("") ? "lambda" : "def " + name + " ";
		return prefix + "(" + Expression.join(formals, ", ") + ") = " + body;
	}
	
	public void extend(AggregateDefn adef) {
		
		List<Pattern> newformals = new LinkedList<Pattern>();
		List<Type> argTypes = new LinkedList<Type>();
		for (Pattern p : formals) {
			/* Strip a toplevel type ascription from every argument pattern */
			if (p instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern)p;
				argTypes.add(tp.t);
				newformals.add(tp.p);
			}
			else {
				newformals = formals;
				argTypes = null;
				break;
			}
		}
		
		if (argTypes != null) { adef.setArgTypes(argTypes); }
		if (resultType != null) { adef.setResultType(resultType); }
		
		adef.addClause(new Clause(newformals, body));
		
		adef.addLocation(getSourceLocation());
		
	}	
}
