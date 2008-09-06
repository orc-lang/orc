package orc.orchard.events;

public class PromptEvent extends JobEvent {
	public String message;
	public int promptID;
	
	public PromptEvent() {}
	
	public PromptEvent(int promptID, String message) {
		this.promptID = promptID;
		this.message = message;
	}
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
