package orc.runtime.regions;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

/**
 * This class does nothing except serve
 * as a root for the execution.
 * @author quark
 */
public class Execution extends Region {
	public Execution() {}
	
	@Override
	protected void onClose() {
		// do nothing
	}
}
