package orc.qos.trace.events;

import orc.trace.TokenTracer.PullTrace;

public class QPullEvent extends QEvent implements PullTrace {

	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
}
