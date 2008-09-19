/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.Serializable;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.runtime.CapabilityException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;
import orc.runtime.nodes.Silent;
import orc.runtime.regions.Execution;
import orc.runtime.regions.Region;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.Callable;
import orc.runtime.values.Future;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;
import orc.trace.TokenTracer;

/**
 * Representation of an active thread of execution. Tokens
 * move over the node graph as they are executed. They contain
 * an environment, and may belong to a group. They also 
 * preserve the call chain and contain a value to be passed
 * to the next token.
 * @author wcook, dkitchin, quark
 */
public final class Token implements Serializable, Comparable<Token>, Locatable {
	/**
	 * Return pointer for a function call.
	 * At one point we used a token for the return pointer,
	 * but that was really an abuse of tokens.
	 * @author quark
	 */
	protected static class Continuation {
		public Node node;
		public Env<Object> env;
		public Continuation continuation;
		public Continuation(Node node, Env<Object> env, Continuation continuation) {
			this.node = node;
			this.env = env;
			this.continuation = continuation;
		}
	}
	
	protected Node node;	
	protected Env<Object> env;
	protected GroupCell group;
	protected Region region;
	protected OrcEngine engine;
	/**
	 * The location of the token in the source code.
	 * This is set whenever the token begins processing a new node.
	 * Why not just use the location of the current node? Because
	 * e.g. during a site call this.node actually points to the next
	 * node, not the current one, so the source location would be
	 * incorrect.
	 */
	protected SourceLocation location;
	protected TokenTracer tracer;
	protected Continuation continuation;
	protected Object result;
	protected boolean alive;
	
	/** Copy constructor */
	protected Token(Node node, Env<Object> env, Continuation continuation, GroupCell group, Region region, Object result, OrcEngine engine, TokenTracer tracer) {
		this.node = node;
		this.env = env;
		this.continuation = continuation;
		this.group = group;
		this.result = result;
		this.engine = engine;
		this.region = region;
		this.alive = true;
		this.tracer = tracer;
		region.add(this);
		setPending();
	}
	
	public Token(Node node, Env<Object> env, GroupCell group, Region region, OrcEngine engine, TokenTracer tracer) {
		this(node, env, null, group, region, null, engine, tracer);
	}

	// Signal that this token is dead
	// Synchronized so that multiple threads don't simultaneously signal
	// a token to die and perform duplicate region removals.
	public synchronized void die() {
		if (alive) {
			alive = false;
			region.remove(this);
			unsetPending();
			tracer.die();
		}
	}
	
	// An unreachable token is always dead.
	public void finalize() {
		die();
	} 
	
	/**
	 * If a token is alive, calls the node to perform the next action.
	 */
	public void process() {
		if (!alive) {
			return;
		} else if (group.isAlive()) {
			node.process(this);
		} else {
			die();
		}
	}

	public Node getNode() {
		return node;
	}

	public GroupCell getGroup() {
		return group;
	}
	
	public Env<Object> getEnvironment() {
		return env;
	}

	public Object getResult() {
		return result;
	}

	public OrcEngine getEngine() {
		return engine;
	}
	
	public Region getRegion() {
		return region;
	}
	
	public Token setResult(Object result) {
		this.result = result;
		return this;
	}
	
	public Token setGroup(GroupCell group) {
		this.group = group;
		return this;
	}

	/**
	 * Migrate the token from one region to another.
	 */
	public Token setRegion(Region region) {
		// the order of operations ensures
		// that we don't close a region prematurely
		region.add(this);
		this.region.remove(this);
		this.region = region;
		return this;
	}
	
	public TokenTracer getTracer() {
		return tracer;
	}
	
	public Token setEnv(Env<Object> e) {
		this.env = e;
		return this;
	}

	/**
	 * Move to a node node
	 * @param node the node to move to
	 * @return returns self
	 */
	public Token move(Node node) {
		this.node = node;
		return this;
	}
	
	/**
	 * Enter a closure by moving to a new node and environment,
	 * and setting the continuation for {@link #leaveClosure()}.
	 */
	public Token enterClosure(Node node, Env<Object> env, Node next) {
		if (next instanceof Return) {
			// handle tail call specially
			continuation = continuation.continuation;
		} else if (next instanceof Silent) {
			// handle silent tail call specially
			continuation = new Continuation(next, this.env, null);
		} else {
			continuation = new Continuation(next, this.env, continuation);
		}
		return setEnv(env).move(node);
	}
	
	/**
	 * Leave a closure by returning to the continuation set by
	 * {@link #enterClosure(Node, Env, Node)}.
	 */
	public Token leaveClosure() {
		setEnv(continuation.env);
		move(continuation.node);
		continuation = continuation.continuation;
		return this;
	}

	/**
	 * Push a new future onto the environment stack
	 * @param f		future to push
	 * @return		self
	 */
	public Token bind(Object f) {
		env = env.add(f);
		return this;
	}

	/**
	 * Pop values off of the environment stack.
	 * Used to leave binding scopes.
	 * @param width number of bindings to leave
	 * @return self
	 */
	public Token unwind(int width) {
		env = env.unwind(width);
		return this;
	}
	
	/**
	 * Lookup a variable in the environment
	 * @param var variable name
	 * @return value, or an error if the variable is undefined
	 */
	public Object lookup(Arg var) {
		return var.resolve(env);
	}
	
	/* TODO: replace this stub with a meaningful order on tokens */
	public int compareTo(Token t) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void debug(String s) {
		engine.debug(s);
	}
	
	public void activate() {
		engine.activate(this);
	}
	
	/*
	 * TODO: Introduce priority on tokens, or an 'isImmediate' predicate on sites,
	 * so that let and 'immediate' sites have priority over other site returns.
	 */
	public void resume(Object object) {
		this.result = object;
		engine.resume(this);
	}
	
	/* A return with no arguments simply returns a signal */
	public void resume() {
		resume(Value.signal());
	}
	
	
	/* This token has encountered an error, and dies. */
	public void error(TokenException problem) {
		problem.setSourceLocation(getSourceLocation());
		tracer.error(problem);
		engine.tokenError(this, problem);
		// die after reporting the error, so the engine
		// isn't halted before it gets a chance to report it
		die();
	}

	/**
	 * Print something (for use by the print and println sites).
	 */
	public void print(String string, boolean newline) {
		tracer.print(string, newline);
		engine.print(string, newline);
	}

	/**
	 * Publish a value to the top level.
	 */
	public void publish() {
		tracer.publish(result);
		engine.publish(result);
	}

	/**
	 * Fork a token.
	 * @see #fork(GroupCell, Region)
	 */
	public Token fork() {
		return fork(group, region);
	}
	
	/**
	 * Fork a token with a specified group and region. By convention, the
	 * original token continues on the left while the new token evaluates the
	 * right (this order is arbitrary, but right-branching ensures fewer tokens
	 * are created with the common left-associative asymmetric combinators).
	 */
	public Token fork(GroupCell group, Region region) {
		return new Token(this.node, this.env, this.continuation,
				group, region,
				this.result, this.engine,
				tracer.fork());
	}

	public void setSourceLocation(SourceLocation location) {
		this.location = location;
		tracer.setSourceLocation(location);
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
	
	public void setPending() {
		engine.addPendingToken(this);
	}
	
	public void unsetPending() {
		engine.removePendingToken(this);
	}
	
	public void requireCapability(String name, boolean ifNull) throws CapabilityException {
		Boolean ok = getEngine().getConfig().hasCapability(name);
		if (ok == null) {
			if (!ifNull) throw new CapabilityException(name);
		} else if (!ok) {
			throw new CapabilityException(name);
		}
	}
}
