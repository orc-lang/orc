package orc.ast.extended.declaration.def;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.expression.AssertType;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.type.ArrowType;
import orc.ast.extended.type.AssertedType;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Def;
import orc.ast.simple.expression.HasType;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;


public class AggregateDef {
	
	protected List<Clause> clauses;
	protected Variable var;
	protected List<String> typeParams;
	protected List<Type> argTypes;
	protected Type resultType;
	protected SourceLocation location;
	
	public AggregateDef() {
		clauses = new LinkedList<Clause>();
		var = new Variable();
	}

	public void addClause(Clause c) {
		clauses.add(c);
	}

	public Variable getVar() { return var; }
	
	public Def simplify() throws CompilationException {
		
		// Create a list of formal arguments for this set of clauses
		// Use the length of the args for the first clause as the length of the formals list.
		int n = clauses.get(0).ps.size();
		
		// and check to make sure every clause has the same number of patterns
		for (Clause c : clauses) {
			if (c.ps.size() != n) {
				CompilationException ce = new CompilationException("Mismatched number of arguments in clauses");
				ce.setSourceLocation(location);
				throw ce;
			}
		}
		
		List<Variable> formals = new LinkedList<Variable>();
		for(int i = 0; i < n; i++) {
			formals.add(new Variable());
		}
	
		
		// Default 'otherwise' clause is the silent expression
		orc.ast.simple.expression.Expression body = Pattern.fail();
		
		// Consider clauses in reverse order 
		for (int i = clauses.size() - 1; i >= 0; i--) {
			Clause c = clauses.get(i);
			// Simplify and prepend this clause to the expression body
			body = c.simplify(formals,body);
		}
		
		if (resultType != null && resultType instanceof AssertedType) {
			AssertedType atype = (AssertedType)resultType;
			resultType = atype.type;
			body = new HasType(body, atype.type, false);
		}
		
		return new orc.ast.simple.expression.Def(var, formals, body, typeParams, argTypes, resultType, location);
	}

	public void setTypeParams(List<String> typeParams) {
		this.typeParams = typeParams;
	}

	public void setResultType(Type resultType) {
		this.resultType = resultType;
	}

	public void setArgTypes(List<Type> argTypes) {
		this.argTypes = argTypes;
	}

	public void addLocation(SourceLocation sourceLocation) {
		if (location == null) {
			location = sourceLocation;
		}
		else {
			location = location.overlap(sourceLocation);
		}
	}
}
