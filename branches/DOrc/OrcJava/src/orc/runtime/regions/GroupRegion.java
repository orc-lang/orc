package orc.runtime.regions;

import java.rmi.RemoteException;

import orc.runtime.Group;
import orc.runtime.values.GroupCell;

public class GroupRegion extends Region {
	private static final long serialVersionUID = 1L;
	private Region parent;
	Group group;
	
	/* Create a new group region with the given parent and coupled group cell */
	public GroupRegion(Region parent, Group group) {
		super();
		this.parent = parent;
		parent.grow();
		this.group = group;
		this.group.setRegion(this);
	}
	
	/**
	 * A GroupRegion may be closed preemptively by a Group, before all of its
	 * inhabitants leave.
	 */
	protected void onClose() {
		group.close();
		parent.shrink();
	}
}
