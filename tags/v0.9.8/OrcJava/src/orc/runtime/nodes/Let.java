package orc.runtime.nodes;

import orc.ast.oil.arg.Arg;
import orc.error.runtime.SiteResolutionException;
import orc.runtime.Token;
import orc.runtime.values.Value;

public class Let extends Node {

	public Arg arg;
	public Node next;
	
	public Let(Arg arg, Node next) {
		this.arg = arg;
		this.next = next;
	}
	
	@Override
	public void process(Token t) {
		Object v;
		try {
			v = Value.forceArg(t.lookup(arg), t);
		} catch (SiteResolutionException e) {
			t.error(e);
			return;
		}
		
		if (v != Value.futureNotReady) {
			t.move(next); t.setResult(v).activate();
		}
	}
	
	public String toString() {
		return "Let(" + arg + ")";
	}
}
