package orc.lib.util;

import kilim.Task;
import kilim.pausable;

/**
 * This example class shows how to write a Java
 * site which uses features of Kilim. See examples/kilim.orc.
 * 
 * @author quark
 */
public class KilimExample {
	private String id;
	public KilimExample(String id) {
		this.id = id;
	}
	
	/** Do not publish */
	public @pausable void exit() {
		Task.exit(id + " exiting");
	}
	
	/** Signal an error */
	public @pausable void error() throws Exception {
		throw new Exception("ERROR");
	}
	
	/** Publish after millis milliseconds. */
	public @pausable String sleep(Number millis) {
		Task.sleep(millis.longValue());
		return id;
	}
	
	/** Send a signal. */
	public void signal() {}
}
