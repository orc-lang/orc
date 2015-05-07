/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.List;

import orc.env.Env;
import orc.error.runtime.ArityMismatchException;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;

/**
 * Represents a standard closure: a function defined in an environment.
 * 
 * Note that a closure is not necessarily a resolved value, since it may contain unbound
 * variables, and therefore cannot be used in arg position until all such variables
 * become bound. 
 * 
 * @author wcook, dkitchin
 */
public class Closure extends Value implements Callable {
	private static final long serialVersionUID = 1L;
	Node body;
	int arity;
	Env env;

	public Closure(int arity, Node body, Env env) {
		this.arity = arity;
		this.body = body;
		this.env = env;
	}

	public void createCall(Token t, List<Object> args, Node nextNode) throws ArityMismatchException {
		if (args.size() != arity) {
			throw new ArityMismatchException(arity, args.size());
		}
		
		t.enterClosure(body, env, nextNode);
		for (Object f : args) t.bind(f);
		t.activate();
	}

	public void setEnvironment(Env env) {
		this.env = env;
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}

