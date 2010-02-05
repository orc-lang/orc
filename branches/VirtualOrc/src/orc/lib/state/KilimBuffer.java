//
// KilimBuffer.java -- Java class KilimBuffer
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

package orc.lib.state;

import java.util.LinkedList;

import kilim.Mailbox;
import kilim.Pausable;

public class KilimBuffer<V> {
	private final LinkedList<Mailbox<V>> waiters = new LinkedList<Mailbox<V>>();
	private final LinkedList<V> buffer = new LinkedList<V>();

	public synchronized void put(final V o) {
		final Mailbox<V> waiter = waiters.poll();
		if (waiter != null) {
			waiter.putnb(o);
		} else {
			buffer.add(o);
		}
	}

	public synchronized V get() throws Pausable {
		final V out = buffer.poll();
		if (out != null) {
			return out;
		} else {
			final Mailbox<V> waiter = new Mailbox<V>();
			waiters.add(waiter);
			return waiter.get();
		}
	}
}
