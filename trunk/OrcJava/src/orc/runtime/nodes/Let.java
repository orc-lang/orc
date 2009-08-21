package orc.runtime.nodes;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.SiteResolutionException;
import orc.runtime.Token;
import orc.runtime.values.Value;

public class Let extends Node {

	public Argument arg;
	public Node next;
	
	public Let(Argument arg, Node next) {
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
	
	public String toString() {
		return "Let(" + arg + ")";
	}
}
