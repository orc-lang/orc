//
// CompilerServiceInterface.java -- Java interface CompilerServiceInterface
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

import orc.orchard.errors.InvalidProgramException;

/**
 * Compile program text into OIL format.
 * 
 * @author quark
 */
public interface CompilerServiceInterface extends Remote {
    /**
     * Compile program text.
     * 
     * @return compiled program
     * @throws InvalidProgramException in case of compilation error.
     */
    public String compile(String devKey, String program) throws InvalidProgramException, RemoteException;
}
