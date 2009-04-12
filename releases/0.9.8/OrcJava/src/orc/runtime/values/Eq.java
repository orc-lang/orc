package orc.runtime.values;

import orc.runtime.sites.core.Equal;

/**
 * Marks value types which can be compared for equivalence in a way that
 * guarantees that equivalent values can be substituted for each other without
 * changing the meaning of a program.
 * 
 * <p>When you implement this, don't forget to override equals and hashCode as well.
 * You should guarantee that if an object is "eqTo" another object it also
 * "equals" that object and has the same hashCode.
 * 
 * @author quark
 */
public interface Eq {
	/**
	 * Return true if this is equivalent to that. that is assumed to be
	 * non-null. This is often implemented in terms of
	 * {@link Equal#eq(Object, Object)}. Two "eq" objects should have
	 * the same hashCode at the time they are compared.
	 */
	public boolean eqTo(Object that);
}
