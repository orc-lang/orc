package orc.trace.values;

import java.io.IOException;
import java.io.Writer;

public class NoneValue extends OptionValue {
	public final static NoneValue singleton = new NoneValue();
	private NoneValue() {}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("None()");
	}
}
