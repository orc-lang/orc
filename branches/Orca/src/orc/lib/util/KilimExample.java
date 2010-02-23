//
// KilimExample.java -- Java class KilimExample
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

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
	private final String id;

	public KilimExample(final String id) {
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
	public String sleep(final Number millis) throws Pausable {
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
	public void signal() {
	}
}
