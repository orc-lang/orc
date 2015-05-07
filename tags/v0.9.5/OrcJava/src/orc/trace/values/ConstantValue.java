package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import xtc.util.Utilities;

/**
 * Constant (not just immutable, but atomic) value,
 * such as a String, Number, Boolean, or Character.
 * @author quark
 */
public class ConstantValue extends AbstractValue {
	public final Serializable constant;
	public ConstantValue(final Serializable constant) {
		super();
		this.constant = constant;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		if (constant instanceof String) {
			out.write('"' + Utilities.escape((String)constant, Utilities.JAVA_ESCAPES) + '"');
		} else out.write(constant.toString());
	}
	public boolean equals(Object that) {
		return that instanceof ConstantValue
				&& ((ConstantValue)that).constant.equals(constant);
	}
}
