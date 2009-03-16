package orc.qos.trace.events;

public class QForkEvent extends QEvent {
	public QForkEvent() {
	}
	
	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString() {
		return "fork";
	}
}
