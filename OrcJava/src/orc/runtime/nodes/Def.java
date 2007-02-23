/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.values.Value;
import java.io.*;

/**
 * Compiled node for a reference to a defined name.
 * @author mbickford
 */
public class Def implements Param, Serializable {
	private static final long serialVersionUID = 1L;
	String name;
	Node next;

	public Def(String name) {
		this.name = name;
		}
	
	/** 
	 * A definition is always bound, so never wait.
	 * @see orc.runtime.nodes.Param#waitOnUnboundVar(orc.runtime.Token)
	 */
	public boolean waitOnUnboundVar(Token t,OrcEngine engine) {
		return false;
	}

	/** 
	 * Looks up the value in of the definition in the environment.
	 * @see orc.runtime.nodes.Param#getValue(orc.runtime.Token)
	 */
	public Value getValue(Token env) {
		return env.lookup(name);
	}
	
	public String toString() {
		return "def{" + name +"}";
	}
	
}
