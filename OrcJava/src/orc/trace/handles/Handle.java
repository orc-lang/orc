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
 * @see HandleOutputStream
 * @author quark
 */
public abstract class Handle<E> implements Externalizable {
	protected E value;
	/** Required by {@link Externalizable}. */
	public Handle() {}
	/**
	 * @param last true if this is the last reference to the event which
	 * will be serialized.
	 */
	public Handle(E value) {
		assert(value != null);
		this.value = value;
	}
	public final E get() {
		return value;
	}
	@Override
	public final String toString() {
		return value.toString();
	}
	@Override
	public final boolean equals(Object that) {
		if (that instanceof Handle) {
			return value.equals(((Handle)that).value);
		} else return false;
	}
	@Override
	public final int hashCode() {
		return value.hashCode();
	}
}