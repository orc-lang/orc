package orc.error;

/**
 * 
 * Common ancestor for all compiler constructs carrying (optional) debug information.
 * 
 * @author dkitchin
 *
 */

public class Debug {

	private DebugInfo info = null;

	public DebugInfo getDebugInfo() {
		return (info != null  ?  info  :  DebugInfo.DEFAULT);
	}

	public void setDebugInfo(DebugInfo info) {
		this.info = info;
	}

	public void setDebugInfo(antlr.Token t) {
		this.setDebugInfo(new DebugInfo(t));
	}
	
	/* Copy debug info from any other debuggable object */
	public void setDebugInfo(Debug that) {
		this.setDebugInfo(that.getDebugInfo());
	}
}
