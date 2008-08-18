package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;
import orc.trace.values.Value;

/**
 * Return from a site call.
 * @author quark
 */
public class ResumeEvent extends Event {
	public final Value value;
	public ResumeEvent(ForkEvent thread, Value value) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.value = value;
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		value.prettyPrint(out, indent+1);
		out.write(")");
	}
	public Term getProperty(String key) {
		if (key.equals("value")) return value;
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "resume"; }
}
