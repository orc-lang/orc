//
// Terms.java -- Java class Terms
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

public final class Terms {
	private Terms() {
	}

	public static <T extends Term> void prettyPrintList(final Writer out, final int indent, final Iterable<T> list, final String separator) throws IOException {
		final Iterator<T> it = list.iterator();
		if (it.hasNext()) {
			it.next().prettyPrint(out, indent);
		}
		while (it.hasNext()) {
			out.write(separator);
			it.next().prettyPrint(out, indent);
		}
	}

	public static <T extends Term> void prettyPrintMap(final Writer out, int indent, final Iterable<Map.Entry<String, T>> map) throws IOException {
		out.write("{\n");
		indent++;
		for (final Map.Entry<String, T> entry : map) {
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

	public static void indent(final Writer out, final int indent) throws IOException {
		for (int i = 0; i < indent; ++i) {
			out.write('\t');
		}
	}

	public static String printToString(final Term term) {
		try {
			final StringWriter writer = new StringWriter();
			term.prettyPrint(writer, 0);
			return writer.toString();
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
	}
}
