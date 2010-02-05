//
// MultiTypeException.java -- Java class MultiTypeException
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Feb 5, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime.typing;

import orc.type.Type;

/**
 * 
 *
 * @author dkitchin
 */
public class MultiTypeException extends TypeException {

	public String errorReport;

	public MultiTypeException() {
		this("");
	}
	
	public MultiTypeException(String message) {
		super("All alternatives for multitype failed to typecheck.\n" + message);
		errorReport = message;
	}


	public MultiTypeException addAlternative(Type t, TypeException e) {
		return new MultiTypeException(errorReport + (t + " failed due to error: \n" + e + "\n"));
	}
	
}
