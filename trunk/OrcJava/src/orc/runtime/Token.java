/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.*;
//import java.io.ObjectOutputStream;
//import java.nio.channels.FileLock;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;
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
	protected Environment env;
	protected GroupCell group;
	protected OrcEngine engine;
	Token caller;
	Value result;
	boolean debugMode;

	public Token(Node node, Environment env, Token caller, GroupCell group, Value result, OrcEngine engine) {
		this.node = node;
		this.env = env;
		this.caller = caller;
		this.group = group;
		this.result = result;
		this.engine = engine;
		this.debugMode = engine.debugMode;
	}
	
	

	/**
	 * If a token is alive, calls the node to perform the next action
	 * @param engine
	 */
	public void process() {
		if (group.isAlive())
			node.process(this);
	}

	public Node getNode() {
		return node;
	}

	public GroupCell getGroup() {
		return group;
	}
	
	public Environment getEnvironment() {
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

	
	
	public Token setResult(Value result) {
		this.result = result;
		return this;
	}
	
	
	public Token setGroup(GroupCell group) {
		this.group = group;
		return this;
	}
	
	public Token setEnv(Environment e) {
		this.env = e;
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
	 * Create a copy of the token
	 * @return	new token
	 */
	public Token copy() {
		return new Token(node, env, caller, group, result, engine);
	}

	/**
	 * Extend the environment with a new variable/value pair
	 * @param var	variable name
	 * @param val	value for this variable
	 * @return		self
	 */
	public Token bind(Var var, Future f) {
		//System.out.println("binding " + var + " to " + val);
		env = new Environment(var, f, env);
		return this;
	}
	
	/**
	 * Lookup a variable in the environment
	 * @param var  	variable name
	 * @return		value, or an exception if the variable is undefined
	 */
	public Future lookup(Argument a) {
		if (a instanceof Var)
		{
			return env.lookup((Var)a);
		}
		else 
		{
			return a.asValue();
		}
		
	}

	public Callable call(Argument a) {
		Future f = this.lookup(a);
		return f.forceCall(this);
	}
	
	public Value arg(Argument a) {
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
		if (debugMode)
			{ System.out.println(s); }
//		 System.out.print("[" + token.hashCode() + "] ");
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
