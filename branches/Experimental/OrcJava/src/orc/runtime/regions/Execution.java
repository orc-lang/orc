package orc.runtime.regions;

import orc.runtime.OrcEngine;

public class Execution extends Region {

	OrcEngine engine;
	boolean running;
	
	public Execution(OrcEngine engine) {
		this.engine = engine;
		this.running = true;
	}
	
	public OrcEngine getEngine() { return engine; }
	public boolean isRunning() { return running; }
	
	@Override
	public void close() {
		running = false;
		engine.wake();
	}

}
