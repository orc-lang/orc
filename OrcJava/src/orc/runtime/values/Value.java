package orc.runtime.values;

import java.util.List;

import orc.error.OrcError;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.lib.str.Read;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.sites.java.ObjectProxy;
import xtc.util.Utilities;

/**
 * 
 * A fully evaluated Orc value. This includes sites, tuples, lists, and so on.
 * 
 * However, it does not include unbound or partially-bound values, which are
 * instead in the broader category of Futures.
 * 
 * @author dkitchin
 * 
 */
public abstract class Value {
	/**
	 * Distinguished value used to indicate that a forced value is not ready.
	 * Don't try to use this value in Orc programs. We use this instead of
	 * throwing an exception because I suspect it's much faster for the common
	 * case.
	 */
	public final static Callable futureNotReady = new Callable() {
		public void createCall(Token caller, List<Object> args, Node nextNode)
				throws TokenException {
			throw new OrcError(
					"Value#futureNotReady#createCall should never be called");
		}
	};

	/**
	 * Force a value (which may be a future) in call position. If the future is
	 * not ready, return {@link Value#futureNotReady} and place the token on a
	 * waiting list to be reactivated when the future is ready.
	 * 
	 * <p>
	 * The primary reason to distinguish call and argument values is that a
	 * closure is allowed to have unforced free values in call position, but not
	 * in argument position (lest those unforced free values escape their
	 * lexical context).
	 */
	public static Callable forceCall(Object f, Token t)
			throws UncallableValueException {
		if (f == null) {
			throw new UncallableValueException("Java 'null' is not callable");
		} else if (f instanceof Future) {
			return ((Future) f).forceCall(t);
		} else if (f instanceof Callable) {
			return (Callable) f;
		} else {
			// FIXME: if f is an Orc Value, we are allowing users
			// to call native methods on it. Is that ok? We could
			// easily check and disallow calling of non-Callable Values.
			return ObjectProxy.proxyFor(f);
		}
	}

	/**
	 * Force a value (which may be a future) in argument position. If the future
	 * is not ready, return {@link Value#futureNotReady} and place the token on
	 * a waiting list to be reactivated when the future is ready.
	 */
	public static Object forceArg(Object f, Token t) {
		if (f == null) {
			return f;
		} else if (f instanceof Future) {
			return ((Future) f).forceArg(t);
		} else {
			return f;
		}
	}

	/**
	 * Static function to access the canonical 'unit' value; currently, the
	 * signal value is an empty tuple.
	 */
	public static Value signal() {
		return signal;
	}

	private static Value signal = new TupleValue();

	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	/** Convert any object to its string representation; the inverse of {@link Read}. */
	public static String write(Object v) {
		if (v == null) {
			return "null";
		} else if (v instanceof String) {
			return '"' + Utilities.escape((String)v, Utilities.JAVA_ESCAPES) + '"';
		} else {
			return v.toString();
		}
	}
}
