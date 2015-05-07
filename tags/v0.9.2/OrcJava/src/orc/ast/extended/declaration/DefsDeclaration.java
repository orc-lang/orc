package orc.ast.extended.declaration;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.extended.Clause;
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
		
		
		// Map each definition name to a list of its clauses
		Map<String, List<Clause>> clauses = new TreeMap<String, List<Clause>>();
		
		for(Definition d : defs) {
			String name = d.name;
			
			if (!clauses.containsKey(name)) {
				clauses.put(name, new LinkedList<Clause>());
			}
			
			clauses.get(name).add(new Clause(d.formals, d.body));
		}
		
		// Map the names of each definition to a var
		// This map must be constructed beforehand so that it can be used
		// to substitute these names in each expression body
		Map<NamedVar, Var> fnames = new TreeMap<NamedVar, Var>();
		for(String name : clauses.keySet())
		{
			fnames.put(new NamedVar(name), new Var());
		}
		
		// The new simplified definitions
		List<orc.ast.simple.Definition> newdefs = new LinkedList<orc.ast.simple.Definition>();
		
		
		for(Map.Entry<String, List<Clause>> entry : clauses.entrySet()) 
		{
			String name = entry.getKey();
			List<Clause> cs = entry.getValue();
			
			// Create a list of formal arguments for this set of clauses
			// Use the length of the args for the first clause as the length of the formals list.
			int n = cs.get(0).ps.size();
			
			// and check to make sure every clause has the same number of patterns
			for (Clause c : cs) {
				if (c.ps.size() != n) {
					// TODO: Make this a specific Orc compilation exception
					throw new Error("Mismatched number of patterns in clauses of '" + name + "'");
				}
			}
			
			List<Var> formals = new LinkedList<Var>();
			for(int i = 0; i < n; i++) {
				formals.add(new Var());
			}
		
			
			// Default 'otherwise' clause is the silent expression
			orc.ast.simple.Expression body = new orc.ast.simple.Silent();
			
			// Consider clauses in reverse order 
			// Note: this reverses their order inside the clauses map too, 
			// but we only need to use them once, so it's not a problem.
			Collections.reverse(cs);
			
			for (Clause c : cs) {
				// Simplify and prepend this clause to the expression body
				body = c.simplify(formals,body);
			}
			
			// Mutually bind all names of expressions in this group in the body of this expression
			// This supports implicit mutual recursion
			body = body.suball(fnames);
			
			// Find the bound name of this definition
			Var varname = fnames.get(new NamedVar(name));
			
			// Add this simplified definition to this list of definitions 
			newdefs.add(new orc.ast.simple.Definition(varname, formals, body));
		}
		
		
		

		// Bind all of these definition names in their scope
		orc.ast.simple.Expression newtarget = target.suball(fnames);
		
		
		return new orc.ast.simple.Defs(newdefs, newtarget);
	}
	
}
