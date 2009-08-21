package orc.ast.extended.declaration.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.type.Constructor;
import orc.ast.extended.type.Datatype;
import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.DeclareType;
import orc.ast.simple.expression.WithLocation;
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
				
		// Create a type to encapsulate the type information for this datatype
		Type dt = new Datatype(typename, members, formals);
		
		// Find the Datatype site, which constructs tuples of datasites
		Argument datatypeSite = new orc.ast.simple.argument.Site(orc.ast.sites.Site.DATATYPE);

		// Make a list of string arguments from the constructor names
		List<Argument> labels = new LinkedList<Argument>();
		for (Constructor c : members) {
			labels.add(new orc.ast.simple.argument.Constant(c.name));
		}
		
		// Make the type a singleton type argument to the Datatype site call
		List<Type> typeArgs = new LinkedList<Type>();
		typeArgs.add(dt);
		
		// Create a source expression which generates a tuple of datasites
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
		
		// Add a type declaration scoped to this body
		body = new DeclareType(dt, typename, body);
		
		return new WithLocation(body, getSourceLocation());
		
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
