package orc.runtime.regions;

import orc.runtime.values.GroupCell;

public class GroupRegion extends Region {

	Region parent;
	GroupCell cell;
	
	/* Create a new group region with the given parent and coupled group cell */
	public GroupRegion(Region parent, GroupCell cell) {
		this.parent = parent;
		this.cell = cell;
		this.parent.add(this);
	}
	
	public void close() {
		cell.close();
		parent.remove(this);
	}

}
