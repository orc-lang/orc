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
	
	public JobConfiguration() {}

	public void setDebuggable(boolean debuggable) {
		this.debuggable = debuggable;
	}

	public void setListenable(boolean listenable) {
		this.listenable = listenable;
	}

	public boolean getDebuggable() {
		return debuggable;
	}

	public boolean getListenable() {
		return listenable;
	}
}
