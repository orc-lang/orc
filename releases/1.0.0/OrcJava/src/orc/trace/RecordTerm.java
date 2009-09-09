package orc.trace;

/**
 * A term with properties (like an object).
 * FIXME: need to figure out how to pattern match this.
 * @author quark
 */
public interface RecordTerm extends Term {
	public Term getProperty(String key);
}
