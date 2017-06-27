package orc.run.porce;

import orc.OrcRuntime;
import orc.run.extensions.SimpleWorkStealingScheduler;

public class PorcEContext {
	public final OrcRuntime runtime;
	
	public PorcEContext(OrcRuntime runtime) {
		this.runtime = runtime;
	}
}
