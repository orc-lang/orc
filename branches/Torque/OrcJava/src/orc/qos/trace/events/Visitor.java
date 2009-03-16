package orc.qos.trace.events;

public interface Visitor<E> {
	
	public E visit(QForkEvent e);
	public E visit(QHaltEvent e);
	public E visit(QPublishEvent e);
	public E visit(QPullEvent e);
	public E visit(QReceiveEvent e);
	public E visit(QSendEvent e);
	public E visit(QStartEvent e);
	public E visit(QStoreEvent e);
}
