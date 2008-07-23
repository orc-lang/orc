package orc.orchard;

import java.net.URL;

public class RedirectEvent extends JobEvent {
	public URL url;
	
	public RedirectEvent() {}
	
	public RedirectEvent(URL url) {
		this.url = url;
	}
}
