/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.Serializable;
import java.util.LinkedList;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.runtime.CapabilityException;
import orc.error.runtime.SiteException;
import orc.error.runtime.SiteResolutionException;
import orc.error.runtime.StackLimitReachedError;
import orc.error.runtime.TokenError;
import orc.error.runtime.TokenException;
import orc.error.runtime.TokenLimitReachedError;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;
import orc.runtime.regions.Region;
import orc.runtime.transaction.Transaction;
import orc.runtime.values.Closure;
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
public class Token implements Serializable, Locatable {
	/**
	 * Return pointer for a function call.
	 * At one point we used a token for the return pointer,
	 * but that was really an abuse of tokens.
	 * @author quark
	 */
	private static class Continuation {
		public Node node;
		public Env<Object> env;
		public Continuation continuation;
		public SourceLocation location;
		/** Track the depth of a tail-recursive continuation. */
		public int depth = 1;
		public Continuation(Node node, Env<Object> env, Continuation continuation, SourceLocation location) {
			this.node = node;
			this.env = env;
			this.continuation = continuation;
			this.location = location;
		}
	}
	
	/** The location of the token in the DAG; determines what the token will do next. */
	public Node node;	
	/** The current environment, which determines the values of variables. */
	private Env<Object> env;
	/** Before doing anything, the token checks if its group is alive. If not, it kills itself. This is how forced termination is implemented. */
	private Group group;
	/** A region corresponds to a dynamic scope in a running program. Tokens can enter and leave regions. When all the tokens have left a region, it is closed. This is how termination is detected. */
	private Region region;
	/** Transaction the token is currently involved in. */
	private Transaction trans;
	/** The engine executing the program. */
	private OrcEngine engine;
	/**
	 * The location of the token in the source code.
	 * This is set whenever the token begins processing a new node.
	 * Why not just use the location of the current node? Because
	 * e.g. during a site call this.node actually points to the next
	 * node, not the current one, so the source location would be
	 * incorrect.
	 */
	private SourceLocation location;
	/** Used for tracing the activity of the token. */
	private TokenTracer tracer;
	/** The continuation determines where to return when a token reaches the end of a function call. */ 
	private Continuation continuation;
	/** The value being published by this token. */
	private Object result;
	private boolean alive;
	/** Number of stack frames remaining before hitting the stack size limit. */
	private int stackAvailable;
	/** Each token has an associated logical clock. The token notifies the clock when it becomes quiescent. When all the clock's tokens are quiescent, it can advance logical time. */
	private LogicalClock clock;
	
	/**
	 * Create a new uninitialized token.
	 * You should get tokens from {@link TokenPool}, not
	 * call this directly.
	 */
	Token() {}
	
	/**
	 * Initialize a root token.
	 */
	final void initializeRoot(Node node, Region region, OrcEngine engine, TokenTracer tracer) {
		// create the root logical clock
		LogicalClock clock = new RootLogicalClock(engine);
		initialize(node, new Env<Object>(),
				null, new Group(), region, null,
				null, engine, null,
				tracer, engine.getConfig().getStackSize(),
				clock);
	}
	
	/**
	 * Initialize a forked token.
	 */
	final void initializeFork(Token that, Group group, Region region) {
		initialize(that.node, that.env.clone(),
				that.continuation, group, region, that.trans,
				that.result, that.engine, that.location,
				that.tracer.fork(), that.stackAvailable,
				that.clock);
	}
	
	/**
	 * Free any resources held by this token.
	 */
	final void free() {
		node = null;	
		env = null;
		group = null;
		region = null;
		trans = null;
		engine = null;
		location = null;
		tracer = null;
		continuation = null;
		result = null;
	}
	

	private void initialize(Node node, Env<Object> env, Continuation continuation, Group group, Region region, Transaction trans, Object result, OrcEngine engine, SourceLocation location, TokenTracer tracer, int stackAvailable, LogicalClock clock) {
		this.node = node;
		this.env = env;
		this.continuation = continuation;
		this.group = group;
		this.result = result;
		this.engine = engine;
		this.region = region;
		this.trans = trans;
		this.alive = true;
		this.location = location;
		this.tracer = tracer;
		this.stackAvailable = stackAvailable;
		this.clock = clock;
		region.add(this);
		unsetQuiescent();
	}

	/**
	 * Kill this token.
	 * Should only be called on non-quiescent tokens.
	 * Synchronized because tokens may be killed from site threads.
	 */
	public final synchronized void die() {
		assert(alive);
		alive = false;
		region.remove(this);
		setQuiescent();
		tracer.die();
		engine.pool.freeToken(this);
	}
	
	/**
	 * If a token is alive, calls the node to perform the next action.
	 */
	public final void process() {
		if (!alive) {
			return;
		} else if (group.isAlive()) {
			node.process(this);
		} else {
			die();
		}
	}

	public final Node getNode() {
		return node;
	}

	public final Group getGroup() {
		return group;
	}
	
	public final Env<Object> getEnvironment() {
		return env;
	}

	public final Object getResult() {
		return result;
	}

	public final OrcEngine getEngine() {
		return engine;
	}
	
	public final Region getRegion() {
		return region;
	}
	
	public final Transaction getTransaction() {
		return trans;
	}
	
	public final Token setResult(Object result) {
		this.result = result;
		return this;
	}
	
	public final Token setGroup(Group group) {
		this.group = group;
		return this;
	}

	public final Token setTransaction(Transaction trans) {
		this.trans = trans;
		return this;
	}
	
	/**
	 * Migrate the token from one region to another.
	 */
	public final Token setRegion(Region region) {
		// the order of operations ensures
		// that we don't close a region prematurely
		region.add(this);
		this.region.remove(this);
		this.region = region;
		return this;
	}
	
	
	public final TokenTracer getTracer() {
		return tracer;
	}

	/**
	 * Move to a node node
	 * @param node the node to move to
	 * @return returns self
	 */
	public final Token move(Node node) {
		this.node = node;
		return this;
	}
	
	/**
	 * Enter a closure by moving to a new node and environment,
	 * and setting the continuation for {@link #leaveClosure()}.
	 * @throws StackLimitReachedError 
	 */
	public final Token enterClosure(Closure closure, Node next) throws StackLimitReachedError {
		if (stackAvailable == 0) {
			throw new StackLimitReachedError();
		} else if (stackAvailable > 0) {
			--stackAvailable;
		}
		if (next instanceof Return) {
			// tail call should return directly to our current continuation
			// rather than going through us
			++continuation.depth;
		} else if (next.isTerminal()) {
			// handle terminal (non-returning) continuation specially
			continuation = new Continuation(next, this.env.clone(), null, location);
		} else {
			continuation = new Continuation(next, this.env.clone(), continuation, location);
		}
		tracer.enter(closure);
		this.env = closure.env.clone();
		return move(closure.def.body);
	}
	
	/**
	 * Leave a closure by returning to the continuation set by
	 * {@link #enterClosure(Node, Env, Node)}.
	 */
	public final Token leaveClosure() {
		int depth = continuation.depth;
		this.env = continuation.env.clone();
		move(continuation.node);
		continuation = continuation.continuation;
		tracer.leave(depth);
		if (stackAvailable >= 0) {
			stackAvailable += depth;
		}
		return this;
	}

	/**
	 * Push a new future onto the environment stack
	 * @param f		future to push
	 * @return		self
	 */
	public final Token bind(Object f) {
		env.add(f);
		return this;
	}

	/**
	 * Pop values off of the environment stack.
	 * Used to leave binding scopes.
	 * @param width number of bindings to leave
	 * @return self
	 */
	public final Token unwind(int width) {
		env.unwind(width);
		return this;
	}
	
	/**
	 * Lookup a variable in the environment
	 * @param var variable name
	 * @return value, or an error if the variable is undefined
	 * @throws SiteResolutionException 
	 */
	public final Object lookup(Arg var) throws SiteResolutionException {
		return var.resolve(env);
	}
	
	public final void debug(String s) {
		engine.debug(s);
	}
	
	public final void activate() {
		engine.activate(this);
	}
	
	public final void resume(Object object) {
		this.result = object;
		engine.resume(this);
	}
	
	/* A return with no arguments simply returns a signal */
	public final void resume() {
		resume(Value.signal());
	}
	
	private final SourceLocation[] getBacktrace() {
		LinkedList<SourceLocation> out = new LinkedList<SourceLocation>();
		out.add(location);
		for (Continuation c = continuation; c != null; c = c.continuation) {
			out.add(c.location);
		}
		return out.toArray(new SourceLocation[0]);
	}
	
	/* This token has encountered an error, and dies. */
	public final void error(TokenException problem) {
		problem.setSourceLocation(getSourceLocation());
		problem.setBacktrace(getBacktrace());
		tracer.error(problem);
		engine.tokenError(problem);
		// die after reporting the error, so the engine
		// isn't halted before it gets a chance to report it
		die();
		// if this is an unrecoverable error, terminate the whole engine
		if (problem instanceof TokenError) engine.terminate();
	}

	/**
	 * Print something (for use by the print and println sites).
	 */
	public final void print(String string, boolean newline) {
		tracer.print(string, newline);
		engine.print(string, newline);
	}

	/**
	 * Publish a value to the top level.
	 */
	public final void publish() {
		tracer.publish(result);
		engine.publish(result);
	}

	/**
	 * Fork a token.
	 * @throws TokenLimitReachedError 
	 * @see #fork(Group, Region)
	 */
	public final Token fork() throws TokenLimitReachedError {
		return fork(group, region);
	}
	
	/**
	 * Fork a token with a specified group and region. By convention, the
	 * original token continues on the left while the new token evaluates the
	 * right (this order is arbitrary, but right-branching ensures fewer tokens
	 * are created with the common left-associative asymmetric combinators).
	 * @throws TokenLimitReachedError 
	 */
	public final Token fork(Group group, Region region) throws TokenLimitReachedError {
		Token out = engine.pool.newToken();
		out.initializeFork(this, group, region);
		return out;
	}

	public final void setSourceLocation(SourceLocation location) {
		this.location = location;
		tracer.setSourceLocation(location);
	}

	public final SourceLocation getSourceLocation() {
		return location;
	}
	
	public final void unsetQuiescent() {
		clock.addActive();
	}
	
	public final void setQuiescent() {
		clock.removeActive();
	}
	
	public final void requireCapability(String name, boolean ifNull) throws CapabilityException {
		Boolean ok = getEngine().getConfig().hasCapability(name);
		if (ok == null) {
			if (!ifNull) throw new CapabilityException(name);
		} else if (!ok) {
			throw new CapabilityException(name);
		}
	}

	public final void delay(int delay) {
		clock.addEvent(delay, this);
	}
	
	public final void pushLtimer() {
		LogicalClock old = this.clock;
		clock = new NestedLogicalClock(old);
		clock.addActive();
		old.removeActive();
	}
	
	public final void popLtimer() throws SiteException {
		LogicalClock old = this.clock;
		clock = old.pop();
		clock.addActive();
		old.removeActive();
	}
}
