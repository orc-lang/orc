package orc.runtime.nodes;

import orc.ast.oil.arg.Arg;
import orc.runtime.Token;
import orc.runtime.values.Value;

public class Let extends Node {

	Arg arg;
	Node next;
	
	public Let(Arg arg, Node next) {
		this.arg = arg;
		this.next = next;
	}
	
	@Override
	public void process(Token t) {
		Object v = Value.forceArg(t.lookup(arg), t);
		
		if (v != Value.futureNotReady) {
			t.move(next); t.setResult(v).activate();
		}
	}

}
