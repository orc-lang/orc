package orc.runtime.regions;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

public class Execution extends Region {

	OrcEngine engine;
	boolean running;
	
	public Execution(OrcEngine engine) {
		this.engine = engine;
		this.running = true;
	}
	
	public OrcEngine getEngine() { return engine; }
	
	@Override
	public void close(Token closer) {
		running = false;
		engine.terminate();
	}

}
