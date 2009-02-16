package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.TokenTracer.PullTrace;

/**
 * This is just a way to uniquely identify a pull. It should preceed the
 * corresponding fork event.
 * 
 * @author quark
 */
public class PullEvent extends Event implements PullTrace {
	@Override
	public String getType() {
		return "pull";
	}
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
