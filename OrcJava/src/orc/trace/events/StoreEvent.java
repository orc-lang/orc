package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.values.Value;
import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;

/**
 * Store a value to a future. Should be followed by a {@link FreeEvent} which
 * indicates that all the effects of setting the future have been recorded.
 * 
 * @author quark
 */
public class StoreEvent extends Event {
	public Value value;
	public StoreEvent(ForkEvent thread, Value value) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.value = value;
	}
	@Override
	public void prettyPrint(Writer out) throws IOException {
		super.prettyPrint(out);
		out.write("(");
		value.prettyPrint(out, 1);
		out.write(")");
	}
}
