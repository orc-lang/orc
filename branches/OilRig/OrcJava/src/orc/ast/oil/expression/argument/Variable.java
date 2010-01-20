package orc.ast.oil.expression.argument;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;


/**
 * Bound variables, represented using deBruijn indices.
 * 
 * These occur in argument and expression position.
 * 
 * @author dkitchin
 *
 */

public class Variable extends Argument implements Comparable<Variable> {
	private static final long serialVersionUID = 1L;
	
	public int index;
	
	public Variable(int index) {
		this.index = index;
	}
	
	@Override
	public Object resolve(Env<Object> env) {
		return resolveGeneric(env);
	}

	public <T> T resolveGeneric(Env<T> env) {
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
	public Type typesynth(TypingContext ctx) throws TypeException {
		Type t = ctx.lookupVar(index);
		
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
		return (v instanceof Variable)
			&& ((Variable)v).index == index;
	}

	public int compareTo(Variable o) {
		return ((Integer)index).compareTo(o.index);
	}

	@Override
	public orc.ast.xml.expression.argument.Argument marshal() throws CompilationException {
		return new orc.ast.xml.expression.argument.Variable(index);
	}
}