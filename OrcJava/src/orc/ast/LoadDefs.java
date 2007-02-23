/**
 * 
 */
package orc.ast;

import java.util.ArrayList;
import java.util.List;

import orc.runtime.Environment;
import orc.runtime.nodes.Return;
import orc.runtime.values.Closure;

/**
 * @author dkitchin
 *
 * An environment transformer which compiles a list of mutually recursive definitions
 * and adds it to an environment.
 *
 */
public class LoadDefs implements EnvBinder {

	List<orc.ast.Definition> defs;
	
	public LoadDefs(List<orc.ast.Definition> defs)
	{
		this.defs = defs;
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.EnvBinder#bind(orc.runtime.Environment)
	 */
	public Environment bind(Environment env) {
		//(Node output,List<orc.ast.Definition> defsin) {
		List<orc.runtime.nodes.Definition> defNodes = new ArrayList<orc.runtime.nodes.Definition>();
		
		for (orc.ast.Definition d : defs) {
			d.resolveNames(new ArrayList<String>(), new ArrayList<String>()); 
			defNodes.add(new orc.runtime.nodes.Definition(d.name, d.formals, 
					                                      d.body.compile(new Return(),new ArrayList<orc.ast.Definition>())));
		}
		
		List<Closure> cs = new ArrayList<Closure>();
		
		for (orc.runtime.nodes.Definition d : defNodes){
		   // create a recursive closure
		   Closure c = new Closure(d.formals, d.body, null/*empty environment*/);
		   cs.add(c);
		   env = new Environment(d.name, c, env);
		}
		for (Closure c : cs){
		  c.setEnvironment(env);
		}
		
		return env;
	}

}
