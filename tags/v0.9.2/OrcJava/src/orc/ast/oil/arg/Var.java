package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.runtime.nodes.Node;
import orc.runtime.values.Future;
import orc.runtime.values.Value;


/**
 * Bound variables, represented using deBruijn indices.
 * 
 * These occur in argument and expression position.
 * 
 * @author dkitchin
 *
 */

public class Var extends Arg {
	private static final long serialVersionUID = 1L;
	
	public int index;
	
	public Var(int index) {
		this.index = index;
	}

	@Override
	public Future resolve(Env<Future> env) {
		return env.lookup(this.index);
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		if (index >= depth) {
			indices.add(index - depth);
		}
	}
	
	public String toString() {
		return "[#" + index + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}