package orc.trace.events;

/**
 * Used to perform a type-case on events.
 * @author quark
 *
 * @param <V> return type of visitor
 */
public interface Visitor<V> {
	public V visit(AfterEvent event);
	public V visit(BeforeEvent event);
	public V visit(BlockEvent event);
	public V visit(ChokeEvent event);
	public V visit(DieEvent event);
	public V visit(ErrorEvent event);
	public V visit(ForkEvent event);
	public V visit(FreeEvent event);
	public V visit(PrintEvent event);
	public V visit(PublishEvent event);
	public V visit(PullEvent event);
	public V visit(ReceiveEvent event);
	public V visit(SendEvent event);
	public V visit(StoreEvent event);
	public V visit(UnblockEvent event);
}
