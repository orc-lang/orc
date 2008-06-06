package orc.orchard.rmi;

import java.io.Serializable;

public class JobConfiguration implements orc.orchard.interfaces.JobConfiguration, Serializable {
	private boolean debuggable = false;
	private boolean listenable = false;
	private String protocol = "Java RMI";

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
