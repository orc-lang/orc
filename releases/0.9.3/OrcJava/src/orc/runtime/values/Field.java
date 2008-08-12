package orc.runtime.values;

import java.io.Serializable;

/**
 * Allow field names to be distinguished from strings when appropriate.
 * @author quark
 */
public class Field extends Value implements Serializable {
	private static final long serialVersionUID = 1L;
	private String key;
	public Field(String key) {
		this.key = key;
	}
	public String getKey() {
		return key;
	}
	public String toString() {
		return super.toString() + "(" + key +")";
	}
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}