package orc.runtime.values;

import java.io.Serializable;

/**
 * Distinguished representation for field names.
 * @author quark
 */
public class Field extends Value implements Serializable, Eq {
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
		if (that == null) return false;
		return eqTo(that);
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	public boolean eqTo(Object that) {
		return (that instanceof Field)
			&& key.equals(((Field)that).key);
	}
}