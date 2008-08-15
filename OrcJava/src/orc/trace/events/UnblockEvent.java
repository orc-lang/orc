package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.Value;

public class UnblockEvent extends Event {
	public UnblockEvent(ForkEvent thread) {
		super(new RepeatHandle<ForkEvent>(thread));
	}
	@Override
	public void prettyPrint(Writer out) throws IOException {
		super.prettyPrint(out);
	}
}
