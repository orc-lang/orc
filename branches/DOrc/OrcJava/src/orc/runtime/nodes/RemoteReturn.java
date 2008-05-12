/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.rmi.RemoteException;

import orc.error.OrcException;
import orc.runtime.Token;

/**
 * The end of a remote computation.
 * @author quark
 */
public class RemoteReturn extends Node {
	public void process(final Token t) {
		// The main difference between this and a regular return
		// is that we launch a separate thread to perform the return.
		// This is to avoid deadlock in the case where we end up returning
		// to ourselves.
		new Thread(new Runnable() {
			public void run() {
				try {
					t.getCaller().returnResult(t.getResult());
				} catch (RemoteException e) {
					t.error(new OrcException(e));
				}
				t.die();
			}
		}).start();
	}
}