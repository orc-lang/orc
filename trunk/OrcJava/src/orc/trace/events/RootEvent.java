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
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		// do nothing
	}
}
