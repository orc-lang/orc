package orc.orchard.events;

public interface Visitor<E> {
	public E visit(PrintlnEvent event);
	public E visit(PromptEvent event);
	public E visit(PublicationEvent event);
	public E visit(RedirectEvent event);
	public E visit(TokenErrorEvent event);
}
