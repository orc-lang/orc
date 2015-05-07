package orc.trace;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import orc.trace.values.Value;

public final class Terms {
	private Terms() {}

	public static <T extends Term> void prettyPrintList(Writer out, int indent, Iterable<T> list, String separator) throws IOException {
		Iterator<T> it = list.iterator();
		if (it.hasNext()) {
			it.next().prettyPrint(out, indent);
		}
		while (it.hasNext()) {
			out.write(separator);
			it.next().prettyPrint(out, indent);
		}	
	}
	
	public static <T extends Term> void prettyPrintMap(Writer out, int indent, Iterable<Map.Entry<String, T>> map) throws IOException {
		out.write("{\n");
		indent++;
		for (Map.Entry<String,T> entry : map) {
			indent(out, indent);
			out.write(entry.getKey());
			out.write(": ");
			entry.getValue().prettyPrint(out, indent);
			out.write("\n");
		}
		indent--;
		indent(out, indent);
		out.write("}");
	}

	public static void indent(Writer out, int indent) throws IOException {
		for (int i = 0; i < indent; ++i) out.write('\t');
	}
	
	public static String printToString(Term term) {
		try {
			StringWriter writer = new StringWriter();
			term.prettyPrint(writer, 0);
			return writer.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
