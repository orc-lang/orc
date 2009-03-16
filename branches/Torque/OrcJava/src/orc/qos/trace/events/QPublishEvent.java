package orc.qos.trace.events;

public class QPublishEvent extends QEvent {

	Object publishValue;
	
	public QPublishEvent(Object val) {
		publishValue = val;
	}
	
	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString() {
		return "pub: "+publishValue;
	}
}
