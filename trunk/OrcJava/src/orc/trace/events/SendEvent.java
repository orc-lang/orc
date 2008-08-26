package orc.trace.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.Term;
import orc.trace.Terms;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.TupleValue;
import orc.trace.values.Value;

public class SendEvent extends Event {
	public final Value site;
	public final TupleValue arguments;
	public SendEvent(Value site, Value[] arguments) {
		this.site = site;
		this.arguments = new TupleValue(arguments);
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "site", site);
		prettyPrintProperty(out, indent, "arguments", arguments);
	}
	@Override
	public Term getProperty(String key) {
		if (key.equals("site")) return site;
		else if (key.equals("arguments")) return arguments;
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "send"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
