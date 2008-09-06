package orc.orchard.events;

import java.net.URL;

public class RedirectEvent extends JobEvent {
	public URL url;
	
	public RedirectEvent() {}
	
	public RedirectEvent(URL url) {
		this.url = url;
	}
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
