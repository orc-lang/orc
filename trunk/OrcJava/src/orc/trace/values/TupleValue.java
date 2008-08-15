package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

public class TupleValue extends AbstractValue {
	public final Value[] values;
	public TupleValue(final Value[] values) {
		super();
		this.values = values;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("(");
		prettyPrintList(out, indent+1, Arrays.asList(values), ", ");
		out.write(")");
	}
}
