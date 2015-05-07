package orc.ast.extended.declaration.def;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.extended.Visitor;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.expression.HasType;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.TypedPattern;
import orc.ast.extended.type.Type;
import orc.error.compiletime.CompilationException;

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

public class DefMemberClause extends DefMember {

	public List<List<Pattern>> formals;
	public Expression body;
	public Type resultType; // May be null
	
	public DefMemberClause(String name, List<List<Pattern>> formals, Expression body, Type resultType)
	{
		this.name = name;	/* name is "" when used for anonymous functions */
		this.formals = formals;
		this.body = body;
		this.resultType = resultType;
	}
	public String toString() {
		return (name.equals("") ? "lambda" : "def ") + sigToString() + " = " + body;
	}
	
	public String sigToString() {
		StringBuilder s = new StringBuilder();
		
		s.append(name);
		for (List<Pattern> ps : formals) {
			s.append('(');	
				s.append(Expression.join(ps, ","));
			s.append(')');
		}
		
		return s.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public void extend(AggregateDef adef) throws CompilationException {
		
		List<Pattern> phead = formals.get(0);
		List<Pattern> newformals = new LinkedList<Pattern>();
		List<Type> argTypes = new LinkedList<Type>();
		
		for (Pattern p : phead) {
			/* Strip a toplevel type ascription from every argument pattern */
			if (p instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern)p;
				argTypes.add(tp.t);
				newformals.add(tp.p);
			}
			else {
				newformals = phead;
				
				/* There is at least one argument with a missing annotation.
				 * Request inference.
				 */
				argTypes = null;
				
				break;
			}
		}
		if (argTypes != null) { adef.setArgTypes(argTypes); }
		
		
		
		Expression newbody = body;
		
		if (formals.size() > 1) {
			List<List<Pattern>> ptail = formals.subList(1, formals.size());
			if (resultType != null) {
				newbody = new HasType(newbody, resultType);
			}
			newbody = Expression.uncurry(ptail, newbody);
		}
		
		
		if (resultType != null) { adef.setResultType(resultType); }
		
		adef.addClause(new Clause(newformals, newbody));
		
		adef.addLocation(getSourceLocation());
		
	}	
}
