/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.error.OrcException;
import orc.error.OrcRuntimeTypeException;
import orc.runtime.values.Value;

/**
 * Representation of an active thread of execution. Tokens move over the node
 * graph as they are executed. They contain an environment, and may belong to a
 * group. They also preserve the call chain and contain a value to be passed to
 * the next token.
 * 
 * None of these methods need to be synchronized because there is only one party
 * responsible for mutating a token. I.e. if it is waiting on a group cell, only
 * that group cell will call its methods.
 * 
 * @author wcook
 */
public interface RemoteToken extends Remote {
	/**
	 * Signal that this token is dead.
	 */
	public void die() throws RemoteException;
	/**
	 * Place this token back into the active queue.
	 */
	public void activate() throws RemoteException;
	/**
	 * Return a result to this token from a function call.
	 * Because function calls may return multiple times, this
	 * token is left untouched to receive future returns, and
	 * a copy is given the result and activated.
	 */
	public void returnResult(Value result) throws RemoteException;
	/**
	 * Return a result to this token from a site call.
	 */
	public void resume(Value value) throws RemoteException;
	/**
	 * Signal an error at this token.
	 */
	public void error(OrcException e) throws RemoteException;
	/**
	 * Return a signal to this token from a site call.
	 */
	public void resume() throws RemoteException;
	/**
	 * Create and return a new logical clock for this token's engine.
	 */
	public LogicalClock newClock() throws RemoteException;
}