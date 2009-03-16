package orc.qos.trace.events;

public class QStartEvent extends QEvent {

	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString() {
		return "start";
	}
}
