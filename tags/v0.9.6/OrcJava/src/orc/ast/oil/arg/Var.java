package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
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

public class Var extends Arg implements Comparable<Var> {
	private static final long serialVersionUID = 1L;
	
	public int index;
	
	public Var(int index) {
		this.index = index;
	}

	@Override
	public <T> T resolve(Env<T> env) {
		try {
			return env.lookup(this.index);
		} catch (LookupFailureException e) {
			throw new OrcError(e);
		}
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
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		Type t;
		try {
			t = ctx.lookup(index);
		} catch (LookupFailureException e) {
			throw new OrcError(e);
		}
		
		if (t != null) {
			return t;
		}
		else {
			throw new TypeException("Could not infer sufficient type information about this variable. " +
									"It may be a recursive function lacking a return type ascription.");
		}
	}
	
	@Override
	public int hashCode() {
		return index + getClass().hashCode();
	}
	
	@Override
	public boolean equals(Object v) {
		if (v == null) return false;
		return (v instanceof Var)
			&& ((Var)v).index == index;
	}

	public int compareTo(Var o) {
		return Integer.signum(index - o.index);
	}
}