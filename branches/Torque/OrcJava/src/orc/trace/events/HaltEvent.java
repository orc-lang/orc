package orc.trace.events;

import orc.trace.TokenTracer.HaltTrace;

/**
 * This is just a way to uniquely identify a halt.
 * EXPERIMENTAL.
 * 
 * @author quark,srosario
 */
public class HaltEvent extends Event implements HaltTrace {
	@Override
	public String getType() {
		return "halt";
	}
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
