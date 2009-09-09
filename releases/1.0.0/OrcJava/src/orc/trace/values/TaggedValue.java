package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.Terms;

public class TaggedValue extends AbstractValue {
	// FIXME: we should serialize the tag id somehow
	public final String tagName;
	public final Value[] values;
	public TaggedValue(final String tagName, final Value[] values) {
		super();
		this.tagName = tagName;
		this.values = values;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(tagName + "(");
		Terms.prettyPrintList(out, indent+1, Arrays.asList(values), ", ");
		out.write(")");
	}
	public boolean equals(Object value) {
		if (!(value instanceof TaggedValue)) return false;
		TaggedValue that = (TaggedValue)value;
		if (!that.tagName.equals(tagName)) return false;
		if (that.values.length != values.length) return false;
		for (int i = 0; i < values.length; ++i) {
			if (!values[i].equals(that.values[i])) return false;
		}
		return true;
	}
}
