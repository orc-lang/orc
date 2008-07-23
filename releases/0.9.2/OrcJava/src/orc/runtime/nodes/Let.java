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
		Value v = t.lookup(arg).forceArg(t);
		
		if (v == null) 
			{ return; /* This token must wait for a variable to become bound */ }
		else 
			{ t.move(next); t.activate(v); }
	}

}
