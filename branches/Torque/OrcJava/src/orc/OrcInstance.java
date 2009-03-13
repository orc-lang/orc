package orc;

import java.util.concurrent.BlockingQueue;

import orc.env.Env;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;

public class OrcInstance implements Runnable {
	
	OrcEngine engine;
	Node root;
	BlockingQueue<Object> q;
	boolean running = false;
	
	public OrcInstance(OrcEngine engine, Node root, BlockingQueue<Object> q) {
		this.engine = engine;
		this.root = root;
		this.q = q;
	}

	public void run() {
		running = true;
		engine.run(root);
		running = false;
	}
	
	public BlockingQueue<Object> pubs() { 
		return q; 
	}
	
	public boolean isRunning() {
		return running;
	}

}
