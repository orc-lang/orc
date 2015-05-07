/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;


import orc.runtime.Token;
import orc.runtime.values.GroupCell;

/**
 * Compiled node used to store the value of a binding in a where clause.
 * @author wcook
 */
public class Store extends Node {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Gets the group of the token and sets its value to be the result
	 * of the input token. 
	 * As a side effect of setting the value of a group, a pull variable
	 * becomes bound and the execution of the group is terminated.
	 */
	public void process(Token t) {
		/*
		if (engine.debugMode)
			engine.debug("Store/Stop " + var + "=" + t.getResult(), t);
		*/
		GroupCell group = t.getGroup();
		group.setValue(t);
		t.die();
	}
}
