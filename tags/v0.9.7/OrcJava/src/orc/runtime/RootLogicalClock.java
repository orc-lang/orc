package orc.runtime;

import orc.error.runtime.SiteException;

/** A nested logical clock. */
public class RootLogicalClock extends LogicalClock {
	private OrcEngine engine;
	RootLogicalClock(OrcEngine engine) {
		super();
		this.engine = engine;
	}
	
	@Override
	protected void setQuiescent() {
		super.setQuiescent();
		engine.terminate();
	}

	@Override
	LogicalClock pop() throws SiteException {
		throw new SiteException("Cannot pop root logical clock.");
	}
}
