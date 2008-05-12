/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.nodes;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import orc.RemoteOrcServer;
import orc.ast.simple.arg.Argument;
import orc.error.OrcException;
import orc.runtime.RemoteToken;
import orc.runtime.Token;
import orc.runtime.values.Constant;
import orc.runtime.values.Future;
import orc.runtime.values.FutureUnboundException;
import orc.runtime.values.Value;

/**
 * Comiled node for a remote expression. This behaves almost exactly like a call
 * to a closure, except that the body of the closure is determined statically,
 * and of course the fact that the body is evaluated on a different Orc engine.
 * 
 * @author quark
 */
public class RemoteCall extends Node {
	Argument server;
	Node body;
	Node next;

	public RemoteCall(Argument server, Node body, Node next) {
		this.server = server;
		this.body = body;
		this.next = next;
	}

	/**
	 * Send the token to be executed on the remote node.
	 */
	public void process(Token t) {
		// Make sure the server argument is ready
		Future f = t.lookup(server);
		Value v;
		try {
			v = f.forceArg(t);
		} catch (FutureUnboundException e1) {
			return;
		}
		
		// Get the server to pass the token to
		RemoteOrcServer server;
		try {
			server = (RemoteOrcServer)((Constant)v).getValue();
		} catch (ClassCastException e) {
			t.error(new OrcException(e));
			return;
		}
		
		// Create a return token; this uses the same
		// approach as Closure to return directly to
		// the caller's caller if possible.
		RemoteToken caller;
		if (next instanceof RemoteReturn) {
			caller = t.getCaller(); // tail-call
			// the caller should already have been
			// registered as a remote object
		} else if (next instanceof Return) {
			caller = t.getCaller(); // tail-call
			try {
				// Register the caller so it can receive results
				// from RemoteReturn
				UnicastRemoteObject.exportObject(caller, 0);
			} catch (ExportException e) {
				// We assume this means that the object was already
				// exported, so we can ignore this error.
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		} else {
			caller = t.copy().move(next); // normal call
			((Token)caller).die();
			try {
				// Register the caller so it can receive results
				// from RemoteReturn; since we just created this
				// token we know it's not registered already.
				UnicastRemoteObject.exportObject(caller, 0);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}
		}
		try {
			server.run(t.freeze(body, caller));
		} catch (RemoteException e) {
			t.error(new OrcException(e));
		}
	}
	public String toString() {
		return super.toString() + "(" + server +")";
	}
}