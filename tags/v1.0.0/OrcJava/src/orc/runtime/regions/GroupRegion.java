package orc.runtime.regions;

import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.trace.events.Event;

public final class GroupRegion extends SubRegion {
	GroupCell cell;
	
	/* Create a new group region with the given parent and coupled group cell */
	public GroupRegion(Region parent, GroupCell cell) {
		super(parent);
		this.cell = cell;
	}
	
	protected void onClose() {
		super.onClose();
		cell.close();
	}
}
