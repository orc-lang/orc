package orc.qos.trace.events;

import orc.trace.TokenTracer.StoreTrace;

public class QStoreEvent extends QEvent implements StoreTrace {
	Object storeValue;
	
	public QStoreEvent(Object storeValue) {
		this.storeValue=storeValue;
	}
	
	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString() {
		return "store: "+storeValue.toString();
	}
}
