package orc.runtime.nodes;

import orc.runtime.IsolatedGroup;
import orc.runtime.Token;
import orc.runtime.regions.IsolatedRegion;

/**
 * Enter an isolated region (protected from forced termination).
 * See also {@link Unisolate}.
 * @author quark
 */
public class Isolate extends Node {
	private static final long serialVersionUID = 1L;
	public Node body;
	public Isolate(Node body) {
		this.body = body;
	}

	public void process(Token t) {
		t.setRegion(new IsolatedRegion(t.getEngine().getExecution(), t.getRegion()));
		t.setGroup(new IsolatedGroup(t.getGroup()));
		t.move(body).activate();
	}
}
