package orc.trace.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.handles.RepeatHandle;
import orc.trace.values.AbstractValue;

/**
 * Spawning a new thread.
 * @author quark
 */
public class ForkEvent extends Event {
	public static ForkEvent ROOT = new ForkEvent() {
		public void prettyPrint(Writer out) throws IOException {
			out.write("ForkEvent.ROOT");
			out.write("(");
			out.write(Integer.toHexString(hashCode()));
			out.write(")");
		}
	};
	/** For {@link #ROOT} */
	private ForkEvent() { super(null); }
	public ForkEvent(ForkEvent thread) {
		super(new RepeatHandle<ForkEvent>(thread));
	}
	@Override
	public void prettyPrint(Writer out) throws IOException {
		super.prettyPrint(out);
		out.write("(");
		out.write(label());
		out.write(")");
	}
}
