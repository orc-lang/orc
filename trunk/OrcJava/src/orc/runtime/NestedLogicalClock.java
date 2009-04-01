package orc.runtime;

import orc.error.runtime.SiteException;

/** A nested logical clock. */
public class NestedLogicalClock extends LogicalClock {
	/** Logical timers form a tree structure. As long as children
	 * are not quiescent, the parent timer cannot advance. */
	private LogicalClock parent;
	NestedLogicalClock(LogicalClock parent) {
		super();
		this.parent = parent;
	}
	
	@Override
	protected void setQuiescent() {
		super.setQuiescent();
		parent.removeActive();
	}
	
	@Override
	protected void unsetQuiescent() {
		super.unsetQuiescent();
		parent.addActive();
	}

	@Override
	LogicalClock pop() throws SiteException {
		return parent;
	}
}
