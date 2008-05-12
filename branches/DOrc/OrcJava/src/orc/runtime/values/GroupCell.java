/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.RemoteCellGroup;
import orc.runtime.Token;

/**
 * A value container that is associated with a group. There should be at least
 * one group cell per server per group (typically exactly one). In order for
 * this to work, the implementation is split into a serializable part (which
 * holds the value) and a transient, non-serializable manager (which keeps track
 * of the tokens waiting on the value at its node). When the cell is copied
 * between servers, it creates a new manager to manage tokens on that server.
 * 
 * @author quark
 */
public class GroupCell implements Future {
	/**
	 * The non-serializable half of a group cell. Manages the token waiting list
	 * and receives messages from the master group.
	 */
	private class GroupCellManager implements RemoteGroupCell {
		private List<Token> waitList = new LinkedList<Token>();
		public GroupCellManager() {
			try {
				UnicastRemoteObject.exportObject(this, 0);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
		public synchronized void die() {
			state = State.DEAD;
			for (Token t : waitList) t.die();
			waitList = null;
		}
		public synchronized void setValue(Value value) {
			GroupCell.this.value = value;
			state = State.BOUND;
			for (Token t : waitList) t.activate();
			waitList = null;
		}
		public synchronized Value getValue(Token t) throws FutureUnboundException {		
			switch (state) {
			case NEW:
				try {
					setValue(group.getValue(this));
					return value;
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					throw new RuntimeException(e);
				} catch (FutureUnboundException e) {
					if (!e.alive) {
						die();
						return getValue(t);
					} else {
						state = State.WAITING;
						// fall through
					}
				}
			case WAITING:
				waitList.add(t);
				throw new FutureUnboundException(true);
			case BOUND:
				return value;
			case DEAD:
				t.die();
				throw new FutureUnboundException(false);
			}
			throw new AssertionError("Unexpected state.");
		}
	}
	
	private enum State { NEW, WAITING, BOUND, DEAD };
	private State state = State.NEW;
	private Value value = null;
	transient private GroupCellManager manager = null;
	
	private RemoteCellGroup group;

	public GroupCell(RemoteCellGroup group) {
		this.group = group;
		manager = new GroupCellManager();
	}
	
	/**
	 * Serialization should be synchronized.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		synchronized (manager) {
			out.defaultWriteObject();
		}
	}

	/**
	 * When a group cell is copied to a new server it needs to create a new
	 * manager.
	 */
	private void readObject(ObjectInputStream in)
		throws IOException,	ClassNotFoundException
	{
		in.defaultReadObject();
		manager = new GroupCellManager();
		// while we were being copied to the new server,
		// the group may have gotten a value, so we need
		// to check for one
		if (state == State.WAITING) state = State.NEW;
	}

	public Value forceArg(Token t) throws FutureUnboundException {
		return manager.getValue(t).forceArg(t);
	}
	
	public Callable forceCall(Token t) throws FutureUnboundException {
		return manager.getValue(t).forceCall(t);
	}
}