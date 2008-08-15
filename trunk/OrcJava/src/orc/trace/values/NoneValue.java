package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

public class NoneValue extends AbstractValue {
	public final static NoneValue singleton = new NoneValue();
	private NoneValue() {}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("None()");
	}
}
