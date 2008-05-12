package orc.runtime.regions;

import java.rmi.RemoteException;

import orc.runtime.OrcEngine;

public class Execution extends Region {
	private static final long serialVersionUID = 1L;
	OrcEngine engine;

	public Execution(OrcEngine engine) {
		super();
		this.engine = engine;
	}

	@Override
	protected void onClose() {
		engine.terminate();
	}
}
