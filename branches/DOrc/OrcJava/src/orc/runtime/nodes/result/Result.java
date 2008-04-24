package orc.runtime.nodes.result;

import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.values.Value;

public abstract class Result extends Node {

	@Override
	public void process(Token t) {
		Value val = t.getResult();	
		emit(val);
		t.die();
	}

	public abstract void emit(Value v);
	
}
