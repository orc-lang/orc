package orc.trace.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;
import orc.trace.query.Terms;
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
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		site.prettyPrint(out, indent+1);
		arguments.prettyPrint(out, indent+1);
		out.write(")");
	}
	public Term getProperty(String key) {
		if (key.equals("site")) return site;
		else if (key.equals("arguments")) return arguments;
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "send"; }
}
