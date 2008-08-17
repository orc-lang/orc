package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.Value;

/**
 * Thread is blocked waiting for a Future. TODO: include a link to some
 * corresponding Where event.
 * 
 * @author quark
 */
public class BlockEvent extends Event {
	public BlockEvent(ForkEvent thread) {
		super(new RepeatHandle<ForkEvent>(thread));
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
	}
}
