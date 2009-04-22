package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import xtc.util.Utilities;

/**
 * Constant (not just immutable, but atomic) value,
 * such as a String, Number, Boolean, Character, or null.
 * @author quark
 */
public class ConstantValue extends AbstractValue {
	public final Serializable constant;
	public ConstantValue(final Serializable constant) {
		super();
		this.constant = constant;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(orc.runtime.values.Value.write(constant));
	}
	public boolean equals(final Object that) {
		if (that == null) return false;
		if (!(that instanceof ConstantValue)) return false;
		final ConstantValue cv = (ConstantValue)that;
		if (cv.constant == null) return constant == null;
		return cv.constant.equals(constant);
	}
}
