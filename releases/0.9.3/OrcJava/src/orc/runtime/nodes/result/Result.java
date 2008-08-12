package orc.runtime.nodes.result;

import orc.runtime.Token;
import orc.runtime.nodes.Node;

public abstract class Result extends Node {

	@Override
	public void process(Token t) {
		Object val = t.getResult();	
		emit(val);
		t.die();
	}

	public abstract void emit(Object v);
	
}
