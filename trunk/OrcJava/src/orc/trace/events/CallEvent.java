package orc.trace.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.handles.RepeatHandle;
import orc.trace.query.Terms;
import orc.trace.values.Value;

public class CallEvent extends Event {
	public final Value site;
	public final Value[] arguments;
	public CallEvent(ForkEvent thread, Value site, Value[] arguments) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.site = site;
		this.arguments = arguments;
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		site.prettyPrint(out, indent+1);
		out.write("(");
		Terms.prettyPrintList(out, 1, Arrays.asList(arguments), ",");
		out.write(")");
		out.write(")");
	}
}
