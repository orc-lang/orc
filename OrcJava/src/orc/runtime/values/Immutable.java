package orc.runtime.values;

import orc.runtime.sites.core.Equal;

/**
 * Marks value types which can
 * be compared for equivalence
 * in a way that guarantees that
 * equivalent values can be substituted
 * for each other without changing the
 * meaning of a program.
 * 
 * @author quark
 */
public interface Immutable {
	/** 
	 * Return true if this is equivalent to that.
	 * that is assumed to be non-null.
	 * This is often implemented in terms of
	 * {@link Equal#equivalent(Object, Object)}.
	 */
	public boolean equivalentTo(Object that);
}
