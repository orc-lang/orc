package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

/**
 * Supertype for traced value representations.
 * @author quark
 */
public interface Value extends Serializable {
	public void prettyPrint(Writer out, int indent) throws IOException;
}
