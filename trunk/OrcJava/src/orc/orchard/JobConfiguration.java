package orc.orchard;

import java.io.Serializable;

/**
 * JAXB does bad things if you extend another class
 * which is not specifically designed to be JAXB-marshalled.
 * So we can't inherit any implementation of this class, which
 * is OK since it's trivial anyways.
 * @author quark
 */
public class JobConfiguration implements Serializable {
	private boolean debuggable = false;
	private boolean listenable = false;
	private String protocol;
	
	public JobConfiguration() {}
	
	public JobConfiguration(String protocol) {
		this();
		setProtocol(protocol);
	}

	public void setDebuggable(boolean debuggable) {
		this.debuggable = debuggable;
	}

	public void setListenable(boolean listenable) {
		this.listenable = listenable;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public boolean getDebuggable() {
		return debuggable;
	}

	public boolean getListenable() {
		return listenable;
	}

	public String getProtocol() {
		return protocol;
	}

}
