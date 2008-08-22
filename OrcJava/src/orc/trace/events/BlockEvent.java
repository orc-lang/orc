package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;
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
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		pull.get().prettyPrint(out, indent+1);
		out.write(")");
	}
	@Override
	public String getType() { return "block"; }
	@Override
	public Term getProperty(String key) {
		if (key.equals("pull")) return pull.get();
		else return super.getProperty(key);
	}
}
