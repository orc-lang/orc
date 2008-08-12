package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;


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
	public Object resolve(Env<Object> env) {
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