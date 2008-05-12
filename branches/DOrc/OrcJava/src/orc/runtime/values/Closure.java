/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.ast.simple.arg.Var;
import orc.error.OrcException;
import orc.error.OrcRuntimeTypeException;
import orc.runtime.Environment;
import orc.runtime.RemoteToken;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;

/**
 * Represents a standard closure: a function defined in an environment.
 * 
 * Note that a closure is not necessarily a Value, since it may contain unbound
 * variables, and therefore cannot be used in arg position until all such variables
 * become bound. 
 * 
 * @author wcook, dkitchin
 */
public class Closure extends Value implements Callable {
	Node body;
	List<Var> formals;
	Environment env;

	public Closure(List<Var> formals, Node body, Environment env) {
		this.body = body;
		this.formals = formals;
		this.env = env;
	}

	/** 
	 * To create a call to a closure, a new token is created using the 
	 * environment in which the closure was defined. This environment is
	 * then extended to bind the formals to the actual arguments.
	 * The caller of the new token is normally a token point to right
	 * after the call. However, for tail-calls the existing caller
	 * is reused, rather than creating a new intermediate stack frame.
	 * @see orc.runtime.values.Callable#createCall(java.lang.String, orc.runtime.Token, java.util.List, orc.runtime.nodes.Node)
	 */
	public void createCall(Token t, List<Future> args, Node nextNode) throws OrcException {
		if (args.size() != formals.size()) {
			throw new OrcRuntimeTypeException("Arity mismatch, expected " + formals.size() + " arguments, got " + args.size() + ".");
		}
		
		// check tail-call optimization
		RemoteToken caller;
		if (nextNode instanceof Return){
			caller = t.getCaller(); // tail-call
		} else {
			// Make a copy of this token which will act as a template for tokens
			// returned from the function.
			caller = t.copy().move(nextNode); // normal call
			// The copy should not count towards the parent region, so we kill
			// it immediately.
			((Token)caller).die();
		}
		
		t.call(body, caller, env);
		for (Var v : formals) t.bind(v, args.remove(0));
		t.activate();
	}

	public void setEnvironment(Environment env) {
		this.env = env;
	}
}