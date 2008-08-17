package orc.trace.query;

import orc.trace.query.patterns.RecordPattern;

/**
 * A term with properties (like an object) which
 * can be deconstructed with {@link RecordPattern}.
 * @author quark
 */
public interface RecordTerm extends Term {
	public Term getProperty(String key);
}
