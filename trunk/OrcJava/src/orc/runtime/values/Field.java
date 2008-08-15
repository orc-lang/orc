package orc.runtime.values;

import java.io.Serializable;

/**
 * Distinguished representation for field names.
 * @author quark
 */
public class Field extends Value implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String key;
	public Field(String key) {
		assert(key != null);
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
	@Override
	public boolean equals(Object that) {
		return key.equals(that);
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
}