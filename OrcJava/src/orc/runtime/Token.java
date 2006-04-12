/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import orc.runtime.nodes.Node;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Value;

/**
 * Representation of an active thread of execution. Tokens
 * move over the node graph as they are executed. They contain
 * an environment, and may be low to a group. They also 
 * preserve the call chain and contain a value to be passed
 * to the next token.
 * @author wcook
 */
public class Token {
	protected Node node;
	protected Environment env;
	protected GroupCell group;
	Token caller;
	Object result;

	public Token(Node node, Environment env, Token caller, GroupCell group, Object result) {
		this.node = node;
		this.env = env;
		this.caller = caller;
		this.group = group;
		this.result = result;
	}

	/**
	 * If a token is alive, calls the node to perform the next action
	 * @param engine
	 */
	public void process(OrcEngine engine) {
		if (group.isAlive())
			node.process(this, engine);
	}

	public Node getNode() {
		return node;
	}

	public GroupCell getGroup() {
		return group;
	}

	public Token setGroup(GroupCell group) {
		this.group = group;
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
		return new Token(node, env, caller, group, result);
	}

	/**
	 * Extend the environment with a new variable/value pair
	 * @param var	variable name
	 * @param val	value for this variable
	 * @return		self
	 */
	public Token bind(String var, Value val) {
		env = new Environment(var, val, env);
		return this;
	}

	/**
	 * Lookup a variable in the environment
	 * @param var  	variable name
	 * @return		value, or an exception if the variable is undefined
	 */
	public Value lookup(String var) {
		return env.lookup(var);
	}

	public Environment getEnvironment() {
		return env;
	}

	public Object getResult() {
		return result;
	}

	public Token setResult(Object result) {
		this.result = result;
		return this;
	}

	public Token getCaller() {
		return caller;
	}
}
