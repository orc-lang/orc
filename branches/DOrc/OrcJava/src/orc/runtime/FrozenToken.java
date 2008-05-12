/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import orc.runtime.nodes.Node;
import orc.runtime.regions.Region;
import orc.runtime.regions.RemoteExecution;
import orc.runtime.regions.RemoteRegion;
import orc.runtime.values.Value;

/**
 * A token frozen so it can be sent between servers in a distributed
 * computation.
 * 
 * @author quark
 */
public class FrozenToken implements Serializable {
	protected Node node;	
	protected Environment env;
	protected RemoteParentGroup group;
	protected RemoteRegion region;
	RemoteToken caller;
	Value result;

	public FrozenToken(Node node, Environment env, RemoteToken caller, RemoteParentGroup group, RemoteRegion region, Value result) {
		this.node = node;
		this.env = env;
		this.caller = caller;
		this.group = group;
		this.result = result;
		try {
			UnicastRemoteObject.exportObject(region, 0);
		} catch (ExportException e) {
			// Assume this means the object was already exported
			// and ignore the error.
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		this.region = region;
	}
	
	public void thaw(OrcEngine engine) {
		// Create a dummy group cell to act as a local proxy for the remote
		// group cell. Because of the hierarchical structure of distributed
		// expressions, this group is guaranteed never to reach a Store
		// node.
		Group group = new Group();
		try {
			if (!this.group.addChild(group)) return;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	
		// Create a dummy region to act as a local proxy
		// for the remote region.
		Region region = new RemoteExecution(this.region);
		
		// Create a new local token
		Token t = new Token(node, env, caller, group, region, result, engine);
		engine.activate(t);
	}
}