package orc.trace.values;

import java.io.IOException;
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
	public boolean equals(Object that) {
		return that instanceof FieldValue
			&& ((FieldValue)that).name.equals(name);
	}
}
