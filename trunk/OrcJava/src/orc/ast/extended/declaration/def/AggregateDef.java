package orc.ast.extended.declaration.def;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.extended.expression.AssertType;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.type.LambdaType;
import orc.ast.extended.type.AssertedType;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.Def;
import orc.ast.simple.type.ArrowType;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;
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
		
		
		boolean asserted = false;
		if (resultType instanceof AssertedType) {
			AssertedType atype = (AssertedType)resultType;
			resultType = atype.type;
			asserted = true;

			
		}
		
		List<List<Type>> dummyArgTypes = new LinkedList<List<Type>>();
		dummyArgTypes.add(argTypes);
		ArrowType converted = (ArrowType)((new LambdaType(dummyArgTypes, resultType, typeParams)).simplify());
		if (typeParams != null) {
			for (int i = 0; i < typeParams.size(); i++) {
				FreeTypeVariable X = new FreeTypeVariable(typeParams.get(i));
				TypeVariable Y = converted.typeParams.get(i);
				body = body.subvar(Y,X);
			}
		}
		
		if (asserted) {
			body = new orc.ast.simple.expression.HasType(body, converted.resultType, false);
		}
		
		
		
		return new orc.ast.simple.expression.Def(var, formals, body, converted.typeParams, converted.argTypes, converted.resultType, location);
	}

	public void setTypeParams(List<String> typeParams) throws CompilationException {
		if (this.typeParams == null) {
			this.typeParams = typeParams;
		}
		else {
			CompilationException ce = new CompilationException("Multiple type parameter definitions");
			ce.setSourceLocation(location);
			throw ce;
		}
		
	}

	public void setResultType(Type resultType) throws CompilationException {
		if (this.resultType == null) {
			this.resultType = resultType;
		}
		else {
			CompilationException ce = new CompilationException("Multiple result type definitions");
			ce.setSourceLocation(location);
			throw ce;
		}
	}

	public void setArgTypes(List<Type> argTypes) throws CompilationException {
		if (this.argTypes == null) {
			this.argTypes = argTypes;
		}
		else if (this.argTypes.size() == 0 && argTypes.size() == 0) {
			// Do nothing.
			// Redefining arg types from empty to empty is fine.
		}
		else { 
			CompilationException ce = new CompilationException("Multiple argument type definitions");
			ce.setSourceLocation(location);
			throw ce;
		}
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
