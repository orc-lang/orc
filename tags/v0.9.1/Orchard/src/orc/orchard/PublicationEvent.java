package orc.orchard;

import orc.orchard.oil.Value;

/**
 * Job publications (published Orc values).
 * @author quark
 */
public class PublicationEvent extends JobEvent {
	public Value value;
	
	public PublicationEvent() {}
	
	public PublicationEvent(Value value) {
		this.value = value;
	}
}