package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;
import xtc.util.Utilities;

/**
 * Printing to stdout.
 * @author quark
 */
public class PrintEvent extends Event {
	public final String output;
	public final boolean newline;
	public PrintEvent(String output, boolean newline) {
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
	public Term getProperty(String key) {
		if (key.equals("output")) return new ConstantValue(output);
		if (key.equals("newline")) return new ConstantValue(newline);
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "print"; }
}
