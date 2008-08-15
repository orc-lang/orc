package orc.trace.values;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

public abstract class AbstractValue implements Value {
	public String toString() {
		try {
			StringWriter writer = new StringWriter();
			prettyPrint(writer, 0);
			return writer.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	public static void indent(Writer out, int indent) throws IOException {
		for (int i = 0; i < indent; ++i) out.write('\t');
	}
	public static void prettyPrintList(Writer out, int indent, Iterable<Value> list, String separator) throws IOException {
		Iterator<Value> it = list.iterator();
		if (it.hasNext()) {
			it.next().prettyPrint(out, indent);
		}
		while (it.hasNext()) {
			out.write(separator);
			it.next().prettyPrint(out, indent);
		}	
	}
}
