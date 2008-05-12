package orc.runtime;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.regions.GroupRegion;
import orc.runtime.values.FutureUnboundException;
import orc.runtime.values.RemoteGroupCell;
import orc.runtime.values.Value;

/**
 * Groups are essential to the evaluation of where clauses: all the tokens that
 * arise from execution of a where definition are associated with the same
 * group. Once a value is produced for the group, all these tokens terminate
 * themselves.
 * 
 * Each group has a group cell for each distributed engine. The group cells
 * are responsible for caching the value locally and maintaining the list of
 * waiting tokens on that engine.
 * 
 * @author wcook, dkitchin, quark
 */
public class Group implements RemoteChildGroup, RemoteParentGroup, RemoteCellGroup {
	/**
	 * Current value. We wouldn't need to cache this here if we sent the value
	 * to all the group cells as soon as it was received. However some of the
	 * group cells may never need the value, so we waste some memory in exchange
	 * for possibly less communication.
	 */
	private Value value = null;
	/** Is the value valid? */
	private boolean bound = false;
	/** Is this group still active? */
	private boolean alive = true;
	/** Subgroups (dependent computations) */
	private List<RemoteChildGroup> children = new LinkedList<RemoteChildGroup>();
	/** Cells which are waiting on a value from this group. */
	private List<RemoteGroupCell> waitList = new LinkedList<RemoteGroupCell>();
	/**
	 * The associated region. Once the group cell has been bound, the region can
	 * be closed without waiting for its tokens to kill themselves, possibly
	 * allowing other regions and groups to be terminated earlier and saving
	 * a bit of memory and useless computation.
	 */
	private GroupRegion region;
	
	public Group() {
		try {
			UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Groups are organized into a tree. In this case a new
	 * subgroup is created and returned.
	 * When we add a new child cell, we can also remove any dead children
	 * so that their memory can be recycled.
	 * @return the new group
	 */
	public synchronized boolean addChild(RemoteChildGroup group) {
		if (!alive) {
			return false;
		} else {
			// FIXME: Is purging really necessary? It seems like pointless
			// overhead in a distributed computation, and saves very little
			// memory.
			//purgeChildren();
			children.add(group);
			return true;
		}
	}
	
	/**
	 * Check for dead children and remove them.
	 */
	private void purgeChildren() {
		for (Iterator<RemoteChildGroup> it = children.iterator(); it.hasNext();) {
			try {
				if (!(it.next().isAlive())) it.remove();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * This call defines the fundamental behavior of groups: When the value is
	 * bound, all subgroups are killed and all waiting tokens are activated.
	 * 
	 * @param value
	 *            the value for the group
	 * @param engine
	 *            engine
	 */
	public synchronized void setValue(Value value) {
		if (!alive) return;
		bound = true;
		this.value = value;
		// kill this group and all subgroups
		kill();
		// transmit the value to all waiting cells
		for (RemoteGroupCell c : waitList) {
			try {
				c.setValue(value);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
		// FIXME: this will cause the group to be closed again, redundantly,
		// but that's not a big deal
		region.close();	
	}
	
	/**
	 * Killing marks this group as dead and closes all subgroups. This does not
	 * affect any waiting cells, which must be dealt with separately.
	 */
	private void kill() {
		if (!alive) return;
		alive = false;
		for (RemoteChildGroup sub : children) {
			try {
				sub.close();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
		children = null;
	}

	/**
	 * Check if a group has been killed
	 * @return true if the group has not been killed
	 */
	public boolean isAlive() {
		return alive;
	}
	
	/**
	 * If the value is available, return it, otherwise add the group cell to a
	 * waiting list if appropriate and throw an exception. If we had
	 * asynchronous RMI, then it would be much simpler to just return the value
	 * by calling setValue or die on the remote cell, but with synchronous
	 * communication that would cause a deadlock.
	 */
	public synchronized Value getValue(RemoteGroupCell c) throws FutureUnboundException {
		if (bound) return value;
		if (alive) {
			// Add the cell to our waitlist
			waitList.add(c);
		}
		throw new FutureUnboundException(alive);
	}
	
	/**
	 * Closing a group kills it (and its subgroups) and also closes any waiting
	 * cells (which kills their tokens).  This may be triggered by the killing
	 * of a parent group, or by the closing of the region associated with
	 * 
	 * GroupCell and GroupRegion refer to each
	 * other. If a region is closed, it kills the associated cell, even if that
	 * cell has not yet been bound.
	 */
	public synchronized void close() {
		if (!alive) return;
		kill();
		for (RemoteGroupCell c : waitList) {
			try {
				c.die();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * GroupCell and GroupRegion refer to each other.
	 * If a cell is bound, it closes the associated region,
	 * even if that region still has live tokens, as an
	 * optimization.
	 */
	public void setRegion(GroupRegion region) {
		this.region = region;
	}
}
