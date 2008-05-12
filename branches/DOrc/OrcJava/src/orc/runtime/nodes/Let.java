package orc.runtime.nodes;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.arg.Argument;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.values.FutureUnboundException;
import orc.runtime.values.Value;

/**
 * While let can be explicitly used like an ordinary site, it is slightly more
 * efficient to avoid this indirection for calls to Let inserted by the compiler
 * (which are pretty frequent). We might want to apply similar treatment to
 * other common sites.
 * 
 * @author quark
 */
public class Let extends Node {
	private List<Argument> args;
	private Node next;

	public Let(List<Argument> args, Node next) {
		this.args = args;
		this.next = next;
	}

	@Override
	public void process(Token t) {
		List<Value> values = new LinkedList<Value>();
		
		try {
			// Convert arguments to values
			for (Argument a : args)	values.add(t.lookup(a).forceArg(t));
		} catch (FutureUnboundException e) {
			// If any are not yet bound, return.
			// The token was already added to a waiting list
			// or killed as appropriate.
			return;
		}
		
		// A let does not have to "resume" like a site call,
		// it can activate its token directly.
		t.move(next)
			.setResult(new Args(values).condense())
			.activate();
	}
}
