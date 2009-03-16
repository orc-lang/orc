package orc.qos.trace.events;

import orc.ast.sites.Site;


public class QSendEvent extends QEvent {
	String siteName;
	public QSendEvent(Object site) {
		siteName = site.getClass().toString();
	}

	public <E> E accept(Visitor<E> v) {
		return v.visit(this);
	}
	
	public String toString() {
		return siteName;
	}
}