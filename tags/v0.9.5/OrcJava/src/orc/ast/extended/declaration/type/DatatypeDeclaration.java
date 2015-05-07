package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.Expression;
import orc.ast.extended.declaration.Declaration;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
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
	
	public DatatypeDeclaration(String typename, List<Constructor> members)
	{
		this.typename = typename;
		this.members = members;
	}
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		for (Constructor c : members) {
		
			NamedVar x = new NamedVar(c.name);
			Var y = new Var();
			int arity = c.args.size();
			
			orc.ast.simple.Expression C = new orc.ast.simple.Call(
											new orc.ast.simple.arg.Site(orc.ast.sites.Site.TAG), 
											new orc.ast.simple.arg.Constant(arity),
											new orc.ast.simple.arg.Constant(c.name)); 
			target = new orc.ast.simple.Where(target.subst(y,x), C, y);
		}
		
		return new WithLocation(target, getSourceLocation());
	}

	public String toString() {
		return "type " + typename + " = " + Expression.join(members, " | ");
	}
}
