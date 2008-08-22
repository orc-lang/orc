package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;

/**
 * Return from a site call. At one point this had a handle to the corresponding
 * site call, but that's not really necessary (the call is just the preceeding
 * {@link SendEvent} in the same thread) and we may not want to bother recording
 * the call (which should be deterministic anyways).
 * 
 * @author quark
 */
public class ReceiveEvent extends Event {
	public final Value value;
	public final int latency;
	public ReceiveEvent(Value value, int latency) {
		this.value = value;
		this.latency = latency;
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		value.prettyPrint(out, indent+1);
		out.write(", ");
		out.write(Integer.toString(latency));
		out.write(")");
	}
	public Term getProperty(String key) {
		if (key.equals("value")) return value;
		if (key.equals("latency")) return new ConstantValue(latency);
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "receive"; }
}
