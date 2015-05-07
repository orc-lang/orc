/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.Serializable;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.SourceLocation;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;
import orc.runtime.regions.Region;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.Callable;
import orc.runtime.values.Future;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;

/**
 * Representation of an active thread of execution. Tokens
 * move over the node graph as they are executed. They contain
 * an environment, and may belong to a group. They also 
 * preserve the call chain and contain a value to be passed
 * to the next token.
 * @author wcook, dkitchin, quark
 */
public class Token implements Serializable, Comparable<Token> {
	private static final long serialVersionUID = 1L;
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
	private SourceLocation location;
	Token caller;
	Object result;
	boolean alive;
	
	public Token(Node node, Env<Object> env, Token caller, GroupCell group, Region region, Object result, OrcEngine engine) {
		this.node = node;
		this.env = env;
		this.caller = caller;
		this.group = group;
		this.result = result;
		this.engine = engine;
		this.region = region;
		this.alive = true;
		region.add(this);
	}
	
	public Token(Node node, Env<Object> env, Execution exec) {
		this(node, env, null, new GroupCell(), exec, null, exec.getEngine());		
	}
	

	// Signal that this token is dead
	// Synchronized so that multiple threads don't simultaneously signal
	// a token to die and perform duplicate region removals.
	public synchronized void die() {
		if (alive) {
			alive = false;
			region.remove(this);
		}
	}
	
	// An unreachable token is always dead.
	public void finalize() { die(); } 
		
	
	/**
	 * If a token is alive, calls the node to perform the next action.
	 */
	public void process() {
		if (!alive) {
			return;
		} else if (group.isAlive()) {
			location = node.getSourceLocation();
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

	public Token getCaller() {
		return caller;
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

	public Token setRegion(Region region) {
		
		// Migrate the token from one region to another
		region.add(this);
		this.region.remove(this);
		
		this.region = region;
		return this;
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
	 * 
	 * Create a copy of this token with the same dynamic characteristics,
	 * but executing at a new point in the graph with a different environment.
	 * Set the new caller's token to the token provided.
	 */
	public Token callcopy(Node node, Env<Object> env, Token returnToken) {
		return new Token(node, env, returnToken, group, region, null, engine);
	}

	/**
	 * Create a copy of the token
	 * @return new token
	 */
	public Token copy() {
		return new Token(node, env, caller, group, region, result, engine);
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
	 * @return
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
	public Object lookup(Arg a) {
		return a.resolve(env);		
	}
	
	/* TODO: replace this stub with a meaningful order on tokens */
	public int compareTo(Token t) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public void debug(String s)
	{
		engine.debug(s);
	}
	
	public void activate()
	{
		engine.activate(this);
	}
	
	public void activate(Object v)
	{
		setResult(v);
		activate();
	}
	
	/*
	 * TODO: Introduce priority on tokens, or an 'isImmediate' predicate on sites,
	 * so that let and 'immediate' sites have priority over other site returns.
	 */
	public void resume(Object object)
	{
		this.result = object;
		engine.resume(this);
	}
	
	/* A return with no arguments simply returns a signal */
	public void resume()
	{
		resume(Value.signal());
	}
	
	
	/* This token has encountered an error, and dies. */
	public void error(TokenException problem) {
		problem.setSourceLocation(location);
		engine.tokenError(this, problem);
		// die after reporting the error, so the engine
		// isn't halted before it gets a chance to report it
		die();
	}
}
