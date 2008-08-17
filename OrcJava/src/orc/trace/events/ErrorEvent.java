package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.error.runtime.TokenException;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.Value;

/**
 * A fatal error in a thread.
 */
public class ErrorEvent extends Event {
	public final TokenException error;
	public ErrorEvent(ForkEvent thread, TokenException error) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.error = error;
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		out.write(error.toString());
		out.write(")");
	}
}
