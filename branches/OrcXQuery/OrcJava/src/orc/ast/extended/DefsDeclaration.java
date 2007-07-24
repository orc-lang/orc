package orc.ast.extended;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.simple.arg.*;

/**
 * A group of defined expressions, together as a declaration. 
 * 
 * Any contiguous sequence of definitions is assumed to be mutually recursive.
 * 
 * The simplification of a group of definitions is complicated by the mutually
 * recursive binding, which requires that each definition bind its name in all of
 * the other definitions.
 * 
 * @author dkitchin
 */

public class DefsDeclaration implements Declaration {

	public List<Definition> defs;
	
	public DefsDeclaration(List<Definition> defs)
	{
		this.defs = defs;
	}
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) {
		
		
		// Map the names of each definition to a var
		// This map must be constructed beforehand so that it can be used
		// to substitute these names in each expression body
		Map<FreeVar, Var> m = new TreeMap<FreeVar, Var>();
		
		for(Definition d : defs)
		{
			m.put(new FreeVar(d.name), new Var());
		}
	
		
		List<orc.ast.simple.Definition> newdefs = new LinkedList<orc.ast.simple.Definition>();
		
		for(Definition d : defs)
		{
			Var name = m.get(new FreeVar(d.name));
			List<Var> formals = new LinkedList<Var>();
			orc.ast.simple.Expression body = d.body.simplify();
			
			
			// Bind formal arguments
			for (String s : d.formals)
			{
				Var v = new Var();
				formals.add(v);
				body.subst(v,new FreeVar(s));
			}
			
			
			
			// Bind the names of all expressions in this recursive group
			body.suball(m);
			
			newdefs.add(new orc.ast.simple.Definition(name, formals, body));
			
		}
		
		
		target.suball(m);
		
		return new orc.ast.simple.Def(newdefs, target);
		
		
	}
	
}
