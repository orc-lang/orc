//
// Callable.java -- Java interface Callable
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.values;

import java.util.List;

import orc.ast.oil.TokenContinuation;
import orc.error.runtime.TokenException;
import orc.runtime.Token;

/**
 * Callable objects include sites and definitions
 * @author wcook
 */
public interface Callable {

	/**
	 * Create a call to a callable value
	 * @param caller	token for which the call is being made: points to the call node
	 * @param args		argument list
	 * @param publishContinuation	next node after the call node, to which the result should be sent
	 */
	void createCall(Token caller, List<Object> args, TokenContinuation publishContinuation) throws TokenException;
}
