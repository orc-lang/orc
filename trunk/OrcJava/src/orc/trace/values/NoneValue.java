package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import orc.trace.query.Frame;
import orc.trace.query.patterns.Pattern;

public class NoneValue extends OptionValue {
	public final static NoneValue singleton = new NoneValue();
	private NoneValue() {}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("None()");
	}
}
