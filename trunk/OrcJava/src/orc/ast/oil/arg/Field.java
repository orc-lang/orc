package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.ground.Message;


/**
 * Field access argument. Embeds a String key.
 * 
 * @author dkitchin
 */

public class Field extends Arg implements Comparable<Field>{
	private static final long serialVersionUID = 1L;
	public String key;
	
	public Field(String key)
	{
		this.key = key;
	}
	
	public Object resolve(Env env) {
		return new orc.runtime.values.Field(key);
	}
	
	public String toString() {
		return "#field(" + key + ")";
	}
	
	public int compareTo(Field that) {
		return this.key.compareTo(that.key);
	}
	
	public boolean equals(Object that) {
		return that.getClass().equals(Field.class) && (this.compareTo((Field)that) == 0);
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		return new Message(this);
	}
	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}
}