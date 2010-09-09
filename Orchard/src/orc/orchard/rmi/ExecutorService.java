//
// ExecutorService.java -- Java class ExecutorService
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import orc.orchard.AbstractExecutorService;
import orc.orchard.api.ExecutorServiceInterface;

public class ExecutorService extends AbstractExecutorService implements ExecutorServiceInterface {
	public ExecutorService(final URI baseURI, final Logger logger) throws RemoteException, MalformedURLException {
		super(logger);
		logger.info("Binding to '" + baseURI + "'");
		UnicastRemoteObject.exportObject(this, 0);
		Naming.rebind(baseURI.toString(), this);
		logger.info("Bound to '" + baseURI + "'");
	}

	public ExecutorService(final URI baseURI) throws RemoteException, MalformedURLException {
		this(baseURI, getDefaultLogger());
	}

	public static void main(final String[] args) {
		URI baseURI = URI.create("rmi://localhost/orchard/executor");
		if (args.length > 0) {
			try {
				baseURI = new URI(args[0]);
			} catch (final URISyntaxException e) {
				System.err.println("Invalid URI '" + args[0] + "'");
				return;
			}
		}
		try {
			new ExecutorService(baseURI);
		} catch (final RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (final MalformedURLException e) {
			System.err.println("Invalid URI '" + args[0] + "'");
			return;
		}
	}
}
