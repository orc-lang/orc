package orc.lib.util;

import java.util.concurrent.Callable;

import kilim.Pausable;
import kilim.Task;
import orc.runtime.Kilim;

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
	public void exit() throws Pausable {
		Task.exit(id + " exiting");
	}
	
	/** Signal an error */
	public void error() throws Exception {
		throw new Exception("ERROR");
	}
	
	/** Publish after millis milliseconds. */
	public String sleep(Number millis) throws Pausable {
		Task.sleep(millis.longValue());
		return id;
	}
	
	public String sleepThread(final Number millis) throws Pausable, Exception {
		Kilim.runThreaded(new Callable<Object>() {
			public Object call() throws InterruptedException {
				Thread.sleep(millis.longValue());
				return null;
			}
		});
		return id;
	}
	
	/** Send a signal. */
	public void signal() {}
}
