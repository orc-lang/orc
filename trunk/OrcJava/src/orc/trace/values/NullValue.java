package orc.trace.values;

import java.io.IOException;
import java.io.Writer;

public class NullValue extends AbstractValue {
	public final static NullValue singleton = new NullValue();
	private NullValue() {}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("Null()");
	}
}
