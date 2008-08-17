package orc.trace.query.patterns;

import java.io.IOException;
import java.io.StringWriter;

import orc.trace.query.Term;

/**
 * This interface exists to distinguish terms
 * which may contain variables.
 * @author quark
 */
public abstract class Pattern implements Term {
	public String toString() {
		try {
			StringWriter writer = new StringWriter();
			prettyPrint(writer, 0);
			return writer.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
