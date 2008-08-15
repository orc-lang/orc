package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

public class FieldValue extends AbstractValue {
	public final String name;
	public FieldValue(final String name) {
		super();
		this.name = name;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(name);
	}
}
