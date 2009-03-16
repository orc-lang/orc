package orc.qos.trace.events;

public class QReceiveEvent extends QEvent {

	Object retValue;
	
	public QReceiveEvent(Object val) {
		retValue = val;
	}
	
	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString(){
		return "RET : "+retValue;
	}
}
