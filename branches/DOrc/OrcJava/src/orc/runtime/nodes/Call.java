/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.arg.Argument;
import orc.error.DebugInfo;
import orc.error.OrcException;
import orc.runtime.Token;
import orc.runtime.values.Callable;
import orc.runtime.values.Future;
import orc.runtime.values.FutureUnboundException;

/**
 * Compiled node for a call (either a site call or a definition call)
 * @author wcook
 */
public class Call extends Node {
	private static final long serialVersionUID = 1L;
	Argument callee;
	List<Argument> args;
	Node next;

	public Call(Argument callee, List<Argument> args, Node next) {
		this.callee = callee;
		this.args = args;
		this.next = next;
	}
	
	/*
	 * TODO: Reintroduce debugging information into the AST so that it reaches here
	public String Label() {
		if (tok == null)
			return name;
		return name + " on line " + tok.getLine();
	}
	*/

	/** 
	 * Looks up the function to be called, then creates a call
	 * token using the argument expressions.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t) {
		Future f = t.lookup(callee);
		Callable target;
		try {
			target = f.forceCall(t);
		} catch (FutureUnboundException e1) {
			/**
			 * the caller is still unbound, in which case the calling token will
			 * be activated when the caller value becomes available. Thus, we
			 * simply return and wait for the token to enter the process method
			 * again.
			 */
			return;
		}
		
		/**
		 * Collect all of the environment's bindings for these args.
		 * Note that some of them may still be unbound, since we are
		 * not forcing the futures.
		 */
		List<Future> actuals = new LinkedList<Future>();
		
		for (Argument a : args)
		{
			actuals.add(t.lookup(a));
		}
		

		try {
			target.createCall(t, actuals, next);
		} catch (OrcException e) {
			DebugInfo info = this.getDebugInfo();
			t.error(info, e);
		}
	}
	public String toString() {
		return super.toString() + "(" + callee + args +")";
	}
}
