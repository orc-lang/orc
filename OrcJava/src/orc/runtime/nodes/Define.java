/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.util.List;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Closure;

/**
 * Compiled node to create a definition
 * @author wcook
 */
public class Define extends Node {

	String name;
	List<String> formals;
	Node body;
	Node next;

	public Define(String name, List<String> formals, Node body, Node next) {
		this.name = name;
		this.formals = formals;
		this.body = body;
		this.next = next;
	}

	/** 
	 * Creates a closure containing the body of the definition. The environment
	 * for the closure is the same as the input environment, but it is extended
	 * to <it>include a binding for the definition name whose value is the closure</it>.
	 * This means that the closure environment must refer to the closure, so there
	 * is a cycle in the object pointer graph. This cycle is constructed in 
	 * three steps:
	 * <nl>
	 * <li>Create the closure with a null environment
	 * <li>Bind the name to the new closure
	 * <li>Update the closure to point to the new environment
	 * </ul>
	 * Then the next token is activated in this new environment.
	 * This is a standard technique for creating recursive closures.
	 * @see orc.runtime.nodes.Node#process(orc.runtime.Token, orc.runtime.OrcEngine)
	 */
	public void process(Token t, OrcEngine engine) {
		if (engine.debugMode)
			engine.debug("Define " + name, t);
		
		// create a recursive closure
		Closure c = new Closure(formals, body, null/*empty environment*/);
		t.bind(name, c);
		c.setEnvironment(t.getEnvironment());
		engine.activate(t.move(next));
	}
}
