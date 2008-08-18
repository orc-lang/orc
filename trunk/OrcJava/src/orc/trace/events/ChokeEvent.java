package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Term;

/**
 * Thread on right-hand side of a where clause was terminated.
 * @author quark
 */
public class ChokeEvent extends Event {
	public Handle<StoreEvent> store;
	public ChokeEvent(ForkEvent thread, StoreEvent store) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.store = new RepeatHandle<StoreEvent>(store);
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		store.get().prettyPrint(out, indent+1);
		out.write(")");
	}
	public Term getProperty(String key) {
		if (key.equals("store")) return store.get();
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "choke"; }
}
