package orc.lib.state;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import kilim.Mailbox;
import kilim.Pausable;

public class KilimBuffer<V> {
	private LinkedList<Mailbox<V>> waiters = new LinkedList<Mailbox<V>>();
	private LinkedList<V> buffer = new LinkedList<V>();
	public synchronized void put(V o) {
		Mailbox<V> waiter = waiters.poll();
		if (waiter != null) waiter.putnb(o);
		else buffer.add(o);
	}
	public synchronized V get() throws Pausable {
		V out = buffer.poll();
		if (out != null) return out;
		else {
			Mailbox<V> waiter = new Mailbox<V>();
			waiters.add(waiter);
			return waiter.get();
		}
	}
}
