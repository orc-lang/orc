package orc.ast.extended.declaration.defn;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.Definition;
import orc.ast.simple.arg.Var;
import orc.error.compiletime.CompilationException;
import orc.type.ArrowType;
import orc.type.Type;

public class AggregateDefn {
	
	protected List<Clause> clauses;
	protected ArrowType type;
	protected Var var;
	
	public AggregateDefn() {
		clauses = new LinkedList<Clause>();
		type = null;
		var = new Var();
	}

	public void addClause(Clause c) {
		clauses.add(c);
	}
	
	public void setType(ArrowType t) {
		type = t;
	}

	public Var getVar() { return var; }
	
	public Definition simplify() throws CompilationException {
		
		/*
		// Make sure a type has been assigned
		if (type == null) {
			// TODO: Make this exception more specific
			throw new CompilationException("Definition missing type ascription");
		}
		*/
		
		// Create a list of formal arguments for this set of clauses
		// Use the length of the args for the first clause as the length of the formals list.
		int n = clauses.get(0).ps.size();
		
		// and check to make sure every clause has the same number of patterns
		for (Clause c : clauses) {
			if (c.ps.size() != n) {
				// TODO: Add name/source information
				throw new CompilationException("Mismatched number of arguments in clauses");
			}
		}
		
		List<Var> formals = new LinkedList<Var>();
		for(int i = 0; i < n; i++) {
			formals.add(new Var());
		}
	
		
		// Default 'otherwise' clause is the silent expression
		orc.ast.simple.Expression body = new orc.ast.simple.Silent();
		
		// Consider clauses in reverse order 
		for (int i = clauses.size() - 1; i >= 0; i--) {
			Clause c = clauses.get(i);
			// Simplify and prepend this clause to the expression body
			body = c.simplify(formals,body);
		}
		
		return new orc.ast.simple.Definition(var, formals, body, type);
	}
}
