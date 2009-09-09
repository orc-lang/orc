package orc.trace.handles;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Works with {@link HandleInputStream} and {@link HandleOutputStream} to
 * explicitly manage the lifetime of serialized pointers. Handles are used
 * in place of non-null pointers. There are four types of handles:
 * <dl>
 * <dt>{@link FirstHandle}</dt>
 * <dd>The first references to a value with at least two references.</dd>
 * <dt>{@link RepeatHandle}</dt>
 * <dd>Neither the first nor the last reference to a value with at least three
 * references.</dd>
 * <dt>{@link LastHandle}</dt>
 * <dd>The last reference to a value with at least two references.</dd>
 * <dt>{@link OnlyHandle}</dt>
 * <dd>The only reference to a value. This is functionally equivalent
 * to a regular pointer.</dd>
 * </dl>
 * 
 * <p>
 * It is up to the programmer to choose the appropriate handle type according to
 * the number of references and the order in which values are serialized. In
 * other words, you're performing your own memory management.
 * 
 * <p>
 * WARNING: DO NOT use this with a regular {@link ObjectOutput} or
 * {@link ObjectInput}. It only works with subtypes of
 * {@link HandleOutputStream} and {@link HandleInputStream}.
 * 
 * <p>
 * FIXME: Handles do not deserialize circular references correctly.
 * This should be straightforward to fix (if a handle detects that it
 * needs to point to a value which is currently being read, it should
 * register with the HandleInputStream to be updated once the value is
 * read), but so far I haven't needed it.
 * 
 * @see HandleOutputStream
 * @author quark
 */
public abstract class Handle<E> implements Externalizable {
	protected E value;
	/**
	 * Required by {@link Externalizable}, but should never be called
	 * when not deserializing.
	 */
	public Handle() {}
	/**
	 * Create a handle to a value, which must be non-null.
	 */
	public Handle(E value) {
		assert(value != null);
		this.value = value;
	}
	/**
	 * Get the value pointed to by this handle.
	 */
	public final E get() {
		return value;
	}
	@Override
	public final String toString() {
		return value.toString();
	}
	/**
	 * Handles are equal if the values they point to are equal.
	 */
	@Override
	public final boolean equals(Object that) {
		if (that instanceof Handle) {
			return value.equals(((Handle)that).value);
		} else return false;
	}
	/**
	 * Handles which point to the same value should use the same hash code.
	 */
	@Override
	public final int hashCode() {
		return Handle.class.hashCode() + value.hashCode();
	}
}