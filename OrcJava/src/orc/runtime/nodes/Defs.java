package orc.runtime.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import orc.ast.oil.arg.Var;
import orc.runtime.Token;
import orc.runtime.values.Closure;
import orc.runtime.values.Future;
import orc.runtime.values.PartialValue;

public class Defs extends Node {

	private static final long serialVersionUID = 1L;

	public List<Def> defs;
	public Node next;
	public Set<Var> freeset;
	
	public Defs(List<Def> defs, Node next, Set<Var> freeset)
	{
		this.defs = defs;
		this.next = next;
		this.freeset = freeset;
	}
	
	/** 
	 * Creates closures encapsulating the definitions and the defining environment. 
	 * The environment for the closure is the same as the input environment, but it is extended
	 * to <it>include a binding for the definition name whose value is the closure</it>.
	 * This means that the closure environment must refer to the closure, so there
	 * is a cycle in the object pointer graph. This cycle is constructed in 
	 * three steps:
	 * <nl>
	 * <li>Create the closure with a null environment
	 * <li>Bind the name to the new closure
	 * <li>Update the closure to point to the new environment
	 * </ul>
	 * Then the next token is activated in this new environment.
	 * This is a standard technique for creating recursive closures.
	 * 
	 * Closures created in this way are protected by a PartialValue object,
	 * preventing them from being used in argument position until all unbound
	 * vars in all definition bodies become bound.
	 */
	public void process(Token t) {
		
		List<Closure> cs = new ArrayList<Closure>();
		
		for (Def d : defs) {
			
		   // create a recursive closure
		   Closure c = new Closure(d.arity, d.body, null/*empty environment*/);
		   cs.add(c);
		   
		   Set<Future> freefutures = new HashSet<Future>();
		   for (Var fv : freeset)
		   {
			   freefutures.add(t.lookup(fv));
		   }
		   
		   t.bind(new PartialValue(c, freefutures));
		}
		
		for (Closure c : cs){
		  c.setEnvironment(t.getEnvironment());
		}
		
		t.move(next).activate();
	}
	
}
