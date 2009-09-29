/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.expression.argument.Argument;
import orc.error.runtime.TokenException;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Value;

/**
 * Compiled node for a call (either a site call or a definition call)
 * @author wcook
 */
public class Call extends Node {
	private static final long serialVersionUID = 1L;
	public Argument callee;
	public List<Argument> args;
	public Node next;

	public Call(Argument callee, List<Argument> args, Node next) {
		this.callee = callee;
		this.args = args;
		this.next = next;	
	}

	/** 
	 * Looks up the function to be called, then creates a call
	 * token using the argument expressions.
	 */
	public void process(Token t) {
		
		try {
			Callable target = Value.forceCall(t.lookup(callee), t);

			/** 
			 * target is null if the callee is still unbound, in which
			 * case the calling token will be activated when the
			 * callee value becomes available. Thus, we simply
			 * return and wait for the token to enter the process
			 * method again.
			 */
			if (target == Value.futureNotReady) { return; }

			/**
			 * Collect all of the environment's bindings for these args.
			 * Note that some of them may still be unbound, since we are
			 * not forcing the futures.
			 */
			List<Object> actuals = new LinkedList<Object>();

			for (Argument a : args) {
				actuals.add(t.lookup(a));
			}

			target.createCall(t, actuals, next);
			
		} catch (TokenException e) {
			// if uncaught,  t.error(e) will be called.
			t.throwRuntimeException(e);
		}
	}
	
	public String toString() {
		return "Call(" + callee + ", " + args + ")";
	}
}
