/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.UncallableValueException;
import orc.runtime.Token;
import orc.runtime.nodes.Defs;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;

/**
 * Represents a standard closure: a function defined in an environment.
 * 
 * <p>A closure is not necessarily a resolved value, since it may contain
 * unbound variables, and therefore cannot be used in arg position until all
 * such variables become bound.
 * 
 * @author wcook, dkitchin, quark
 */
public final class Closure extends Value implements Callable, Future {
	private static final long serialVersionUID = 1L;
	private Node body;
	private int arity;
	private Env env = null;
	private List<Object> free = null;

	/**
	 * The environment should be set later; see {@link Defs}.
	 */
	public Closure(int arity, Node body, List<Object> free) {
		this.arity = arity;
		this.body = body;
		this.free = free;
	}
	
	public void setEnvironment(Env env) {
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
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public Object forceArg(Token t) {
		if (free != null) {
			Iterator<Object> freei = free.iterator();
			while (freei.hasNext()) {
				if (Value.forceArg(freei.next(), t) == Value.futureNotReady) {
					return Value.futureNotReady;
				}
				freei.remove();
			}
			free = null;
		}
		return this;
	}

	public Callable forceCall(Token t) throws UncallableValueException {
		return this;
	}
}