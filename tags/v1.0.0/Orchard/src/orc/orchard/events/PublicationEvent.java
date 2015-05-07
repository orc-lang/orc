package orc.orchard.events;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.orchard.values.Value;

/**
 * Job publications (published Orc values).
 * @author quark
 */
@XmlSeeAlso(value={Value.class})
public class PublicationEvent extends JobEvent {
	public Object value;
	
	public PublicationEvent() {}
	
	public PublicationEvent(Object value) {
		this.value = value;
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}