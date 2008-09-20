package orc.runtime.regions;

import orc.runtime.OrcEngine;
import orc.runtime.Token;

public class Execution extends Region {
	private OrcEngine engine;
	
	public Execution(OrcEngine engine) {
		this.engine = engine;
	}
	
	public OrcEngine getEngine() { return engine; }
	
	@Override
	protected void reallyClose(Token closer) {
		engine.terminate();
	}
}
