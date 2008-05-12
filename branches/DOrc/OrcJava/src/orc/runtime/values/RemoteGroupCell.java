/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.runtime.RemoteToken;

public interface RemoteGroupCell extends Remote {
	public void setValue(Value value) throws RemoteException;
	public void die() throws RemoteException;
}