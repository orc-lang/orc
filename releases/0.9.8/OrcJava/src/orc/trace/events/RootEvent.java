package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.values.ConstantValue;

/**
 * The root event is like a ForkEvent but
 * it is its own thread.
 * @author quark
 */
public class RootEvent extends ForkEvent {
	@Override
	public String getType() { return "root"; }
	@Override
	public ForkEvent getThread() {
		// Since Handles can't serialize circular
		// references, yet, we have to fake the
		// circular reference here.
		return this;
	}
}
