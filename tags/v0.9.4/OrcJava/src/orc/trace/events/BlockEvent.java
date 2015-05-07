package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;

/**
 * Thread is blocked waiting for a Future.
 * 
 * @author quark
 */
public class BlockEvent extends Event {
	public Handle<PullEvent> pull;
	public BlockEvent(PullEvent pull) {
		this.pull = new RepeatHandle<PullEvent>(pull);
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "pull",
				new ConstantValue(pull.get()));
	}
	@Override
	public String getType() { return "block"; }
	@Override
	public Term getProperty(String key) {
		if (key.equals("pull")) return pull.get();
		else return super.getProperty(key);
	}
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
