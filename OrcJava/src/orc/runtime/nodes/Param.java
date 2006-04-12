/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import orc.runtime.Token;
import orc.runtime.values.Value;

/**
 * Interface for parameters to calls.
 * @author wcook
 */
public interface Param {

	/**
	 * Determine if the parameter is unbound in an environment
	 * @param env	the environment containing bindings
	 * @return		true if the parameter is unbound
	 */
	boolean waitOnUnboundVar(Token env);

	/**
	 * Gets the value of a parameter in an environment
	 * @param env	the environment containing bindings
	 * @return		the parameter value
	 */
	Value getValue(Token env);
}
