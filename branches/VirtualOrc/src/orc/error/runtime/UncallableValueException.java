//
// UncallableValueException.java -- Java class UncallableValueException
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

package orc.error.runtime;

/**
 * Exception raised when an uncallable value occurs in call position.
 * 
 * @author dkitchin
 */
public class UncallableValueException extends RuntimeTypeException {

	public UncallableValueException(final String message) {
		super(message);
	}

}
