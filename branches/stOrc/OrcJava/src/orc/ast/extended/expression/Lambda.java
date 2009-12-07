package orc.ast.extended.expression;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.def.AggregateDef;
import orc.ast.extended.declaration.def.Clause;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Variable;
import orc.error.compiletime.CompilationException;


public class Lambda extends Expression {

	public List<List<Pattern>> formals;
	public Expression body;
	public Type resultType; /* optional, may be null */
	
	public Lambda(List<List<Pattern>> formals, Expression body, Type resultType) {
		this.formals = formals;
		this.body = body;
		this.resultType = resultType;
	}

	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		// Create a new aggregate definition
		AggregateDef ad = new AggregateDef();

		// Populate the aggregate with a single clause for this anonymous function
		DefMemberClause singleton = new DefMemberClause("", formals, body, resultType);
		singleton.setSourceLocation(getSourceLocation());
		singleton.extend(ad);
		
		// Make a simple AST definition group with one definition created from the aggregate
		List<orc.ast.simple.expression.Def> defs = new LinkedList<orc.ast.simple.expression.Def>();
		defs.add(ad.simplify());
		
		// Bind the definition in a scope which simply publishes it
		Variable f = ad.getVar();
		return new orc.ast.simple.expression.DeclareDefs(defs, new orc.ast.simple.expression.Let(f));		
	}

	public String toString() {
		return "(lambda (" + join(formals, ", ") + ") = " + body + ")";
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
