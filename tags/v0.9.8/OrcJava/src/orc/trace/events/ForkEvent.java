package orc.trace.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.handles.RepeatHandle;
import orc.trace.values.AbstractValue;
import orc.trace.values.ConstantValue;

/**
 * Spawning a new thread.
 * @author quark
 */
public class ForkEvent extends Event {
	@Override
	public String getType() { return "fork"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
