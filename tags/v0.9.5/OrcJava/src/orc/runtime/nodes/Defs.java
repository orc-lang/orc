package orc.runtime.nodes;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.runtime.Token;
import orc.runtime.values.Closure;
import orc.runtime.values.Future;

public class Defs extends Node {
	private static final long serialVersionUID = 1L;
	public List<Def> defs;
	public Node next;
	/**
	 * Variables defined outside this node which appear in the bodies of
	 * the defs. If the defs are all mutually recursive, this is correct,
	 * otherwise this is overly pessimistic and may force some defs to wait
	 * on variables which they won't use.
	 */
	public Set<Var> free;

	public Defs(List<Def> defs, Node next, Set<Var> free) {
		this.defs = defs;
		this.next = next;
		this.free = free;
	}

	/**
	 * Creates closures encapsulating the definitions and the defining
	 * environment. The environment for the closure is the same as the input
	 * environment, but it is extended to <it>include a binding for the
	 * definition name whose value is the closure</it>. This means that the
	 * closure environment must refer to the closure, so there is a cycle in the
	 * object pointer graph. This cycle is constructed in two steps:
	 * <nl>
	 * <li>Create and bind the closure with a null environment
	 * <li>Update the closure to point to the new environment
	 * </ul>
	 * Then the next token is activated in this new environment. This is a
	 * standard technique for creating recursive closures.
	 * 
	 * <p>
	 * Closures created in this way are prevented them from being used in
	 * argument position until all unbound vars in the closure become bound.
	 * This is necessary to prevent unbound vars from escaping their binding
	 * context.
	 */
	public void process(Token t) {
		// Step 0: find free values in the environment
		List<Object> freeValues = new LinkedList<Object>();
		for (Var v : free) {
			Object value = v.resolve(t.getEnvironment());
			if (value instanceof Future) freeValues.add(value);
		}
		
		// Step 1: create and bind the closures
		Closure[] closures = new Closure[defs.size()];
		int i = 0;
		for (Def d : defs) {
			t.bind(closures[i++] = new Closure(d, freeValues));
		}
		// Now the environment is correct relative to the body

		// Step 2: set the environment of each closure
		i = 0;
		for (Def d : defs) {
			Closure c = closures[i++];
			Env<Object> env = new Env<Object>();
			for (Var v : d.free) env.add(v.resolve(t.getEnvironment()));
			c.env = env;
		}

		t.move(next).activate();
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
