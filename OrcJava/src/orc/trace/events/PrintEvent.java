package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.handles.RepeatHandle;
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
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "output",
				new ConstantValue(output));
		if (newline) {
			prettyPrintProperty(out, indent, "newline",
					new ConstantValue(true));
		}
	}
	@Override
	public Term getProperty(String key) {
		if (key.equals("output")) return new ConstantValue(output);
		if (key.equals("newline")) return new ConstantValue(newline);
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "print"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
