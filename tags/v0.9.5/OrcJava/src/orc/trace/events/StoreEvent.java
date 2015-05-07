package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;
import orc.trace.TokenTracer.StoreTrace;
import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;

/**
 * Store a value to a future. Should be followed by a {@link FreeEvent} which
 * indicates that all the effects of setting the future have been recorded.
 * 
 * @author quark
 */
public class StoreEvent extends Event implements StoreTrace {
	public Value value;
	public Handle<PullEvent> pull;
	public StoreEvent(PullEvent pull, Value value) {
		this.pull = new LastHandle<PullEvent>(pull);
		this.value = value;
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "pull",
				new ConstantValue(pull.get()));
		prettyPrintProperty(out, indent, "value", value);
	}
	@Override
	public Term getProperty(String key) {
		if (key.equals("pull")) return pull.get();
		if (key.equals("value")) return value;
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "store"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
