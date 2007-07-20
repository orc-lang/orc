/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.ast.simple.arg.*;
import orc.runtime.Environment;
import orc.runtime.OrcEngine;
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
	private static final long serialVersionUID = 1L;
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
	 * @see orc.runtime.values.Callable#createCall(java.lang.String, orc.runtime.Token, java.util.List, orc.runtime.nodes.Node, orc.runtime.OrcEngine)
	 */
	public void createCall(Token callToken, List<Future> args, Node nextNode, OrcEngine engine) {
		
		/*
		if (engine.debugMode){
			engine.debug("Call " + label + Tuple.format('(', args, ", ", ')'),
					callToken);
			}
		*/
		
		GroupCell callGroup = callToken.getGroup();
		
		// check tail-call optimization
		Token returnToken;
		
		if (nextNode instanceof Return){
			returnToken = callToken.getCaller(); // tail-call
			}
		else {
			returnToken = callToken.move(nextNode); // normal call
			
			
		}
		
		Token t = new Token(body, env, returnToken, callGroup, null/*value*/, engine);
		
		for (Var v : formals)
		{
			t.bind(v, args.remove(0));
		}
		
		/*
		 * TODO: add error checking for arity mismatch (in either direction)
		if (!args.isEmpty())
		{
			... given too many arguments ...
		}
		catch (NoSuchElementException) { ... too few arguments ... }
		*/
		engine.activate(t);
		
	}

	public void setEnvironment(Environment env) {
		this.env = env;
	}
}

