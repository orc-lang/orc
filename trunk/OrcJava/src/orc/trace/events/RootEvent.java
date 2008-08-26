package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.values.ConstantValue;

/**
 * The root event is like a ForkEvent but
 * it has no thread pointer of its own.
 * @author quark
 */
public class RootEvent extends ForkEvent {
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		// the root event has no properties
		out.write(toString());
	}
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
