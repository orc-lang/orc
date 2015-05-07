package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.declaration.defn.AggregateDefn;
import orc.ast.extended.declaration.defn.Clause;
import orc.ast.extended.declaration.defn.DefnClause;
import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.Type;
import orc.error.compiletime.CompilationException;


public class Lambda extends Expression {

	public List<Pattern> formals;
	public Expression body;
	public Type resultType; /* optional, may be null */
	
	public Lambda(List<Pattern> formals, Expression body) {
		
	}
	
	public Lambda(List<Pattern> formals, Expression body, Type resultType) {
		this.formals = formals;
		this.body = body;
		this.resultType = resultType;
	}

	@Override
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		// Create a new aggregate definition
		AggregateDefn ad = new AggregateDefn();

		// Populate the aggregate with a single clause for this anonymous function
		DefnClause singleton = new DefnClause("", formals, body, resultType);
		singleton.setSourceLocation(getSourceLocation());
		singleton.extend(ad);
		
		// Make a simple AST definition group with one definition created from the aggregate
		List<orc.ast.simple.Definition> defs = new LinkedList<orc.ast.simple.Definition>();
		defs.add(ad.simplify());
		
		// Bind the definition in a scope which simply publishes it
		Var f = ad.getVar();
		return new orc.ast.simple.Defs(defs, new orc.ast.simple.Let(f));		
	}

	public String toString() {
		return "(lambda (" + join(formals, ", ") + ") = " + body + ")";
	}
}
