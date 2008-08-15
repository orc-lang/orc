package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.Value;

/**
 * Resume after a {@link BlockEvent}.
 * @author quark
 */
public class UnblockEvent extends Event {
	public Handle<StoreEvent> store;
	public UnblockEvent(ForkEvent thread, StoreEvent store) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.store = new RepeatHandle<StoreEvent>(store);
	}
	@Override
	public void prettyPrint(Writer out) throws IOException {
		super.prettyPrint(out);
		out.write("(");
		store.get().prettyPrint(out);
		out.write(")");
	}
}
