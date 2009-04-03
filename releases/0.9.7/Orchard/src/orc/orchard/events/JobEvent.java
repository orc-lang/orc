package orc.orchard.events;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({PublicationEvent.class,
	TokenErrorEvent.class,
	PrintlnEvent.class,
	PromptEvent.class,
	RedirectEvent.class})
public abstract class JobEvent implements Serializable {
	public int sequence;
	public Date timestamp;
	public abstract <E> E accept(Visitor<E> visit);
}
