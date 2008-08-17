package orc.trace.query;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

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
}
