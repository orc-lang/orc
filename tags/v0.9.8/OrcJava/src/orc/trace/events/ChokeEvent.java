package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;

/**
 * Thread on right-hand side of a where clause was terminated.
 * @author quark
 */
public class ChokeEvent extends Event {
	public Handle<StoreEvent> store;
	public ChokeEvent(StoreEvent store) {
		this.store = new RepeatHandle<StoreEvent>(store);
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "store",
				new ConstantValue(store.get()));
	}
	@Override
	public Term getProperty(String key) {
		if (key.equals("store")) return store.get();
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "choke"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
