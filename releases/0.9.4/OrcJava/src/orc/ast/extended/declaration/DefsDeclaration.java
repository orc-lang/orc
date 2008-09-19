package orc.ast.extended.declaration;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import orc.ast.extended.Expression;
import orc.ast.extended.declaration.defn.AggregateDefn;
import orc.ast.extended.declaration.defn.Clause;
import orc.ast.extended.declaration.defn.Defn;
import orc.ast.extended.declaration.defn.DefnClause;
import orc.ast.simple.Definition;
import orc.ast.simple.WithLocation;
import orc.ast.simple.arg.*;
import orc.error.compiletime.CompilationException;

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

public class DefsDeclaration extends Declaration {

	public List<Defn> defs;
	
	public DefsDeclaration(List<Defn> defs)
	{
		this.defs = defs;
	}
	
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) throws CompilationException {
		
		
		Map<String, AggregateDefn> dmap = new TreeMap<String, AggregateDefn>(); 
		
		// Aggregate all of the definitions in the list into the map
		for (Defn d : defs) {
			String name = d.name;
			if (!dmap.containsKey(name)) {
				dmap.put(name, new AggregateDefn());
			}
			d.extend(dmap.get(name));
		}
		
		// Associate the names of the definitions with their bound variables
		Map<NamedVar, Var> vmap = new TreeMap<NamedVar, Var>();
		
		for (Entry<String, AggregateDefn> e : dmap.entrySet()) {
			NamedVar x = new NamedVar(e.getKey());
			Var v = e.getValue().getVar();
			vmap.put(x,v);
		}
		
		// Create the new list of simplified definitions,
		// with their names mutually bound.
		
		List<orc.ast.simple.Definition> newdefs = new LinkedList<orc.ast.simple.Definition>();
		
		for (AggregateDefn d : dmap.values()) {
			Definition newd = d.simplify().suball(vmap);
			newdefs.add(newd);
		}
		
		// Bind all of these definition names in their scope
		orc.ast.simple.Expression newtarget = target.suball(vmap);		
		
		return new WithLocation(
				new orc.ast.simple.Defs(newdefs, newtarget),
				getSourceLocation());
	}
	
	public String toString() {
		return Expression.join(defs, "\n");
	}
}
