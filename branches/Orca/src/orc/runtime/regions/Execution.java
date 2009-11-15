package orc.runtime.regions;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * This class does nothing except serve
 * as a root for the execution.
 * @author quark
 */
public final class Execution extends Region {
	private OrcEngine engine;
	public Execution(OrcEngine engine) {
		this.engine = engine;
	}
	
	@Override
	protected void onClose() {
		// do nothing
	}
	
	protected void deactivate() {
		super.deactivate();
		engine.terminate();
	}
}
