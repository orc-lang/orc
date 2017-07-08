package orc.run.porce;

import orc.run.extensions.SimpleWorkStealingScheduler;

public class PorcEContext {
	public final SimpleWorkStealingScheduler scheduler;
	
	public PorcEContext(SimpleWorkStealingScheduler scheduler) {
		this.scheduler = scheduler;
	}
}
