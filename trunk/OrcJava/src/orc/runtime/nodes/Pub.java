package orc.runtime.nodes;

import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.values.Value;

public class Pub extends Node {

	@Override
	public void process(Token t) {
		t.publish();
		t.die();
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public boolean isTerminal() { return true; }
}
