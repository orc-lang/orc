package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.Terms;

public class TupleValue extends AbstractValue {
	public final Value[] values;
	public TupleValue(final Value[] values) {
		super();
		this.values = values;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("(");
		Terms.prettyPrintList(out, indent+1, Arrays.asList(values), ", ");
		out.write(")");
	}
	public boolean equals(Object value) {
		if (!(value instanceof TupleValue)) return false;
		TupleValue that = (TupleValue)value;
		if (that.values.length != values.length) return false;
		for (int i = 0; i < values.length; ++i) {
			if (!values[i].equals(that.values[i])) return false;
		}
		return true;
	}
}
