/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.runtime.Environment;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;
import orc.runtime.nodes.Return;

/**
 * Represents a standard closure: a function defined in an environment
 * 
 * @author wcook
 */
public class Closure extends BaseValue implements Callable {
	private static final long serialVersionUID = 1L;
	Node body;
	List<String> formals;
	Environment env;

	public Closure(List<String> formals, Node body, Environment env) {
		this.body = body;
		this.formals = formals;
		this.env = env;
	}
	public boolean Callable0() {
		return formals.size()==0; 
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
	public void createCall(String label, Token callToken,
			List<Param> args, Node nextNode, OrcEngine engine) {
		if (engine.debugMode){
			engine.debug("Call " + label + Tuple.format('(', args, ", ", ')'),
					callToken);
			}
		
		GroupCell callGroup = callToken.getGroup();
		
		// check tail-call optimization
		Token returnToken;
		
		if (nextNode instanceof Return){
			returnToken = callToken.getCaller(); // tail-call
			}
		else {
			returnToken = callToken.move(nextNode); // normal call
			
			
		}
		Token n = new Token(body, env, returnToken, callGroup, null/*value*/);
		int i = 0;
		int numformals = formals.size();
		for (Param e : args)
			if (i < numformals)
			  n.bind(formals.get(i++), e.getValue(callToken));
			else {
				// It would be good if the label were "F at line xxx" for a call on line xxx.
				System.err.println("Too many arguments (" 
					                 + args.size()+ " > " + numformals +
					                 ") in call to " +
					                 label
					                 );
		        System.err.println("Ignoring argument " + e);
			}
		
		
		
		
		engine.activate(n);
		
	}

	public void setEnvironment(Environment env) {
		this.env = env;
	}
}

