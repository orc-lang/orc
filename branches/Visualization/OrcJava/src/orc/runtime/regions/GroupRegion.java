package orc.runtime.regions;

import orc.runtime.values.GroupCell;

public class GroupRegion extends Region {

	Region parent;
	GroupCell cell;
	boolean open;
	
	/* Create a new group region with the given parent and coupled group cell */
	public GroupRegion(Region parent, GroupCell cell) {
		this.parent = parent;
		this.cell = cell;
		this.parent.add(this);
		this.cell.setRegion(this);
		this.open = true;
	}
	
	/* A GroupRegion may be closed preemptively by a GroupCell, before all of its
	 * inhabitants leave, so we must ensure that the close operations occur only
	 * once using the 'open' flag.
	 */
	public void close() {
		if (open) {
			open = false;
			cell.close();
			parent.remove(this);
		}
		
	}

}
