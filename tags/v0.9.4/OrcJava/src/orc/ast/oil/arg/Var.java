package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;


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
	public <T> T resolve(Env<T> env) {
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

	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
		return ctx.lookup(index);
	}
}