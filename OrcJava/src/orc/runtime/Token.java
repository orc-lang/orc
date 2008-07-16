/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.*;
//import java.io.ObjectOutputStream;
//import java.nio.channels.FileLock;
import orc.ast.oil.arg.Arg;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.OrcException;
import orc.error.SourceLocation;
import orc.error.TokenException;
import orc.error.UncallableValueException;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;
import orc.runtime.regions.Region;
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
 * @author wcook
 */
public class Token implements Serializable, Comparable<Token> {
	private static final long serialVersionUID = 1L;
	protected Node node;	
	protected Env<Future> env;
	protected GroupCell group;
	protected Region region;
	protected OrcEngine engine;
	Token caller;
	Value result;
	boolean alive;
	
	public Token(Node node, Env<Future> env, Token caller, GroupCell group, Region region, Value result, OrcEngine engine) {
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
	
	public Token(Node node, Env<Future> env, Execution exec) {
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
	
	public Env<Future> getEnvironment() {
		return env;
	}

	public Value getResult() {
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

	
	
	public Token setResult(Value result) {
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
	
	public Token setEnv(Env<Future> e) {
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
	public Token callcopy(Node node, Env<Future> env, Token returnToken) {
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
	public Token bind(Future f) {
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
	public Future lookup(Arg a) {
		return a.resolve(env);		
	}

	public Callable call(Arg a) throws UncallableValueException {
		Future f = this.lookup(a);
		return f.forceCall(this);
	}
	
	public Value arg(Arg a) {
		Future f = this.lookup(a);
		return f.forceArg(this);
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
	
	public void activate(Value v)
	{
		this.setResult(v);
		engine.activate(this);
	}
	
	/*
	 * TODO: Introduce priority on tokens, or an 'isImmediate' predicate on sites,
	 * so that let and 'immediate' sites have priority over other site returns.
	 */
	public void resume(Value v)
	{
		this.result = v;
		engine.resume(this);
	}
	
	/* A return with no arguments simply returns a signal */
	public void resume()
	{
		resume(Value.signal());
	}
	
	
	/* This token has encountered an error, and dies. */
	public void error(TokenException problem) {
		engine.tokenError(this, problem);
		// die after reporting the error, so the engine
		// isn't halted before it gets a chance to report it
		die();
	}
}
