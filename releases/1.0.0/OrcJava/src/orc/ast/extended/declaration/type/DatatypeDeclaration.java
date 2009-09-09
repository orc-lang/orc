package orc.ast.extended.declaration.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.DeclareType;
import orc.ast.simple.expression.WithLocation;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;
import orc.error.OrcError;
import orc.error.compiletime.PatternException;
import orc.runtime.sites.Site;

/**
 * Declaration of a variant type. 
 * 
 * Even if the typechecker is not active, this declaration will still create
 * constructor sites which can be used for pattern matching.
 * 
 * @author dkitchin
 */
public class DatatypeDeclaration extends Declaration {

	public String typename;
	public List<Constructor> members;
	public List<String> formals;

	public DatatypeDeclaration(String typename, List<Constructor> members, List<String> formals)
	{
		this.typename = typename;
		this.members = members;
		this.formals = formals;
	}
	
	public orc.ast.simple.expression.Expression bindto(orc.ast.simple.expression.Expression target) {
		
		// Find the Datatype site, which constructs tuples of datasites
		Argument datatypeSite = new orc.ast.simple.argument.Site(orc.ast.sites.Site.DATATYPE);

		// Make a list of string arguments from the constructor names
		List<Argument> labels = new LinkedList<Argument>();
		for (Constructor c : members) {
			labels.add(new orc.ast.simple.argument.Constant(c.name));
		}
		
		// Create a source expression which generates a tuple of datasites
		// It takes a single type argument, D
		List<orc.ast.simple.type.Type> typeArgs = new LinkedList<orc.ast.simple.type.Type>();
		TypeVariable D = new TypeVariable();
		typeArgs.add(D);
		orc.ast.simple.expression.Expression source = new orc.ast.simple.expression.Call(datatypeSite, labels, typeArgs);
		
		// Create a tuple pattern of constructor names as vars
		List<Pattern> ps = new LinkedList<Pattern>();
		for (Constructor c : members) {
			ps.add(new VariablePattern(c.name));
		}
		Pattern p = new TuplePattern(ps);
		Variable s = new Variable();
		PatternSimplifier pv;
		
		try {
			pv = p.process(s);
		} catch (PatternException e) {
			// This should never occur
			throw new OrcError(e);
		}
		
		// Pipe the results of the datatype call through the tuple pattern to the target expression
		orc.ast.simple.expression.Expression body = new orc.ast.simple.expression.Sequential(source, pv.target(s, target), s);
		
		// Substitute type variable D for all the free occurrences of the datatype name in the body
		body = body.subvar(D, new FreeTypeVariable(typename));
		
		// Wrap the whole body with a declaration which declares type D.
		// This is the type fraction of the datatype declaration.
		body = new DeclareType(extractTypePart(), D, body);
		
		return new WithLocation(body, getSourceLocation());
		
	}
	
	
	private orc.ast.simple.type.Datatype extractTypePart() {
		Map<FreeTypeVariable, orc.ast.simple.type.Type> bindings = new TreeMap<FreeTypeVariable, orc.ast.simple.type.Type>();
		
		TypeVariable Recurse = new TypeVariable();
		bindings.put(new FreeTypeVariable(typename), Recurse);
		
		List<TypeVariable> newFormals = new LinkedList<TypeVariable>();
		for (String formal : formals) {
			TypeVariable Y = new TypeVariable();
			FreeTypeVariable X = new FreeTypeVariable(formal);
			newFormals.add(Y);
			bindings.put(X, Y);
		}
				
		/* Reduce each constructor to a list of its argument types.
		 * The constructor names are used separately in the dynamic
		 * semantics to give a string representation for the constructed
		 * values.
		 */
		List<List<orc.ast.simple.type.Type>> cs = new LinkedList<List<orc.ast.simple.type.Type>>();
		for (Constructor con : members) {
			List<orc.ast.simple.type.Type> ts = new LinkedList<orc.ast.simple.type.Type>();
			for (Type t : con.args) {
				// Convert the syntactic type to a true type
				orc.ast.simple.type.Type newT = t.simplify().subMap(bindings);
				// Add it as an entry for the new constructor
				ts.add(newT);
			}
			cs.add(ts);
		}
		
		return new orc.ast.simple.type.Datatype(Recurse, cs, newFormals);
	}
	

	public String toString() {
		return "type " + typename + " = " + Expression.join(members, " | ");
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
