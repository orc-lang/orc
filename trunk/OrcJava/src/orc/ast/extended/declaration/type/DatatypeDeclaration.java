package orc.ast.extended.declaration.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Expression;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.simple.TypeDecl;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.Constructor;
import orc.ast.simple.type.Datatype;
import orc.ast.simple.type.Type;
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
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
				
		// Create a type to encapsulate the type information for this datatype
		Type dt = new Datatype(typename, members, formals);
		
		// Find the Datatype site, which constructs tuples of datasites
		Argument datatypeSite = new orc.ast.simple.arg.Site(orc.ast.sites.Site.DATATYPE);

		// Make a list of string arguments from the constructor names
		List<Argument> labels = new LinkedList<Argument>();
		for (Constructor c : members) {
			labels.add(new orc.ast.simple.arg.Constant(c.name));
		}
		
		// Make the type a singleton type argument to the Datatype site call
		List<Type> typeArgs = new LinkedList<Type>();
		typeArgs.add(dt);
		
		// Create a source expression which generates a tuple of datasites
		orc.ast.simple.Expression source = new orc.ast.simple.Call(datatypeSite, labels, typeArgs);

		
		// Create a tuple pattern of constructor names as vars
		List<Pattern> ps = new LinkedList<Pattern>();
		for (Constructor c : members) {
			ps.add(new VariablePattern(c.name));
		}
		Pattern p = new TuplePattern(ps);
		Var s = new Var();
		PatternSimplifier pv;
		
		try {
			pv = p.process(s);
		} catch (PatternException e) {
			// This should never occur
			throw new OrcError(e);
		}
		
		// Pipe the results of the datatype call through the tuple pattern to the target expression
		orc.ast.simple.Expression body = new orc.ast.simple.Sequential(source, pv.target(s, target), s);
		
		// Add a type declaration scoped to this body
		body = new TypeDecl(dt, typename, body);
		
		return new WithLocation(body, getSourceLocation());
		
	}

	public String toString() {
		return "type " + typename + " = " + Expression.join(members, " | ");
	}
}
