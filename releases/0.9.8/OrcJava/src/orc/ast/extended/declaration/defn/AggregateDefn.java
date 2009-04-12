package orc.ast.extended.declaration.defn;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.AssertType;
import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.Definition;
import orc.ast.simple.HasType;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.ArrowType;
import orc.ast.simple.type.AssertedType;
import orc.ast.simple.type.Type;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;


public class AggregateDefn {
	
	protected List<Clause> clauses;
	protected Var var;
	protected List<String> typeParams;
	protected List<Type> argTypes;
	protected Type resultType;
	protected SourceLocation location;
	
	public AggregateDefn() {
		clauses = new LinkedList<Clause>();
		var = new Var();
	}

	public void addClause(Clause c) {
		clauses.add(c);
	}

	public Var getVar() { return var; }
	
	public Definition simplify() throws CompilationException {
		
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
		
		List<Var> formals = new LinkedList<Var>();
		for(int i = 0; i < n; i++) {
			formals.add(new Var());
		}
	
		
		// Default 'otherwise' clause is the silent expression
		orc.ast.simple.Expression body = Pattern.fail();
		
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
		
		return new orc.ast.simple.Definition(var, formals, body, typeParams, argTypes, resultType, location);
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
