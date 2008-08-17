package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.RepeatHandle;
import orc.trace.values.Value;
import xtc.util.Utilities;

/**
 * Printing to stdout.
 * @author quark
 */
public class PrintEvent extends Event {
	public final String output;
	public final boolean newline;
	public PrintEvent(ForkEvent thread, String output, boolean newline) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.output = output;
		this.newline = newline;
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		out.write('"' + Utilities.escape((String)output, Utilities.JAVA_ESCAPES) + '"');
		if (newline) out.write(", true");
		out.write(")");
	}
}
