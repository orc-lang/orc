package orc.orchard;

public class PromptEvent extends JobEvent {
	public String message;
	public int promptID;
	
	public PromptEvent() {}
	
	public PromptEvent(int promptID, String message) {
		this.promptID = promptID;
		this.message = message;
	}
}
