package orc.qos.trace.events;

import orc.trace.TokenTracer.HaltTrace;



public class QHaltEvent extends QEvent implements HaltTrace {
	public QHaltEvent() {
	}
	
	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString() {
		return "halt";
	}
}
