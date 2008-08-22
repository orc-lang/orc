package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

/**
 * The root event is like a ForkEvent but
 * it has no thread pointer of its own.
 * @author quark
 */
public class RootEvent extends ForkEvent {
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("RootEvent(");
		out.write(label());
		out.write(")");
	}
}
