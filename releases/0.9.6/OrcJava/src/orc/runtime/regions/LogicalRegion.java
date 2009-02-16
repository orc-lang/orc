package orc.runtime.regions;

import orc.runtime.LogicalClock;
import orc.runtime.Token;
import orc.runtime.values.GroupCell;
import orc.trace.events.Event;

public class LogicalRegion extends Region {
	private Region parent;
	private LogicalClock clock;
	
	/* Create a new group region with the given parent and coupled group cell */
	public LogicalRegion(Region parent, LogicalClock clock) {
		this.parent = parent;
		this.clock = clock;
		this.parent.add(this);
	}
	
	protected void reallyClose(Token closer) {
		clock.stop();
		parent.remove(this, closer);
	}

	public Region getParent() {
		return parent;
	}
}
