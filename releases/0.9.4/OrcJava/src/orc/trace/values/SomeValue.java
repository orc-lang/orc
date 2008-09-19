package orc.trace.values;

import java.io.IOException;
import java.io.Writer;

public class SomeValue extends OptionValue {
	public final Value content;
	public SomeValue(final Value content) {
		super();
		this.content = content;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("Some(");
		content.prettyPrint(out, indent+1);
		out.write(")");
	}
}
