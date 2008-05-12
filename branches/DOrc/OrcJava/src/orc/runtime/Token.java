/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Var;
import orc.error.DebugInfo;
import orc.error.OrcException;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Region;
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
public class Token implements RemoteToken, Comparable<Token> {
	private static final long serialVersionUID = 1L;
	private Node node;
	private Environment env;
	private Group group;
	private Region region;
	private OrcEngine engine;
	// FIXME: this is serving double duty for both remote and local calls.
	// It might be clearer and possibly allow some optimization if it was split
	// into caller (for local function calls) and remoteCaller (for remote
	// computations).
	private RemoteToken caller;
	private Value result;
	private boolean alive;

	public Token(Node node, Environment env, RemoteToken caller, Group group, Region region, Value result, OrcEngine engine) {
		this.node = node;
		this.env = env;
		this.caller = caller;
		this.group = group;
		this.result = result;
		this.engine = engine;
		this.region = region;
		this.alive = true;
		region.grow();
	}

	/**
	 * Prepare a token for transit to a remote server.
	 */
	public FrozenToken freeze(Node node, RemoteToken caller) {
		return new FrozenToken(node, env, caller, group, region, result);
	}

	// Synchronized so that multiple threads don't simultaneously signal
	// a token to die and perform duplicate region removals.
	public synchronized void die() {
		if (alive) {
			alive = false;
			region.shrink();
		}
	}
	
	/**
	 * If a token is alive, calls the node to perform the next action.
	 */
	public void process() {
		if (group.isAlive()) {
			debug("Processing token at " + node.toString());
			node.process(this);
		}
		else {
			die();
		}
	}

	public Group getGroup() {
		return group;
	}
	
	public Environment getEnvironment() {
		return env;
	}

	public Value getResult() {
		return result;
	}
	
	public void returnResult(Value result) {
		copy().setResult(result).activate();
	}

	public RemoteToken getCaller() {
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
	
	
	public Token setGroup(Group group) {
		this.group = group;
		return this;
	}

	public Token setRegion(Region region) {
		
		// Migrate the token from one region to another
		region.grow();
		this.region.shrink();
		
		this.region = region;
		return this;
	}
	/**
	 * Move to a node node
	 * @param node  the node to move to
	 * @return  returns self
	 */
	public Token move(Node node) {
		this.node = node;
		return this;
	}
	
	/**
	 * Move to a node and also set a token to return to and new environment.
	 */
	public void call(Node node, RemoteToken caller, Environment env) {
		move(node);
		this.caller = caller;
		this.env = env;
	}

	/**
	 * Create a copy of the token
	 * @return	new token
	 */
	public Token copy() {
		return new Token(node, env, caller, group, region, result, engine);
	}

	/**
	 * Extend the environment with a new variable/value pair
	 * @param var	variable name
	 * @param val	value for this variable
	 * @return		self
	 */
	public Token bind(Var var, Future f) {
		debug("binding " + var + " to " + f);
		env = new Environment(var, f, env);
		return this;
	}
	
	/**
	 * Lookup a variable in the environment
	 * @param var  	variable name
	 * @return		value, or an exception if the variable is undefined
	 */
	public Future lookup(Argument a) {
		if (a instanceof Var) {
			return env.lookup((Var)a);
		} else {
			return a.asValue();
		}
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
	public void error(OrcException problem) {
		error(node.getDebugInfo(), problem);
	}
	
	/* This token has encountered an error, and dies. */
	public void error(DebugInfo info, OrcException problem) {
		// TODO: Pipe debug output through engine rather than directly to console.
		System.out.println();
		System.out.println("Token " + this + " encountered an error. ");
		System.out.println("Problem: " + problem.getMessage());
		System.out.println("Source location: " + info.errorLocation());
		System.out.println();
		problem.printStackTrace();
		die();
	}
	
	public String toString() {
		return super.toString() + "(" + node + ")";
	}

	public LogicalClock newClock() {
		LogicalClock clock = new LogicalClock();
		engine.addClock(clock);
		return clock;
	}
	
	/*
	 * This was used to help diagnose where memory was being used.
	 * It isn't needed now, but might be useful again someday.
	 * If there is only one active token, it can be dumped to a file,
	 * and then execution can resume again after reading the token from the file
	 * and initializing the engine with that token active.
	 * This can be done with the command line arguments -rundump file .
	 * That capability might be useful, too, but isn't being used now.
	 */
	
/*	public void dump(File f) {
		boolean append = true;
		try {
		FileOutputStream fos = new FileOutputStream(f,append);
		FileLock lock = fos.getChannel().lock(0,10,false);
	    ObjectOutputStream out = new ObjectOutputStream(fos);
	    out.writeObject(this);
	    lock.release();
	    out.close();
	    fos.close();
		}
		catch (Exception e) {
			System.err.println("Warning: did not dump token to file");
		}
	}*/
	
}
