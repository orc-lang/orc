//
// OrcException.java -- Java class OrcException
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error;

/**
 * Any exception generated by Orc, during compilation, loading, or execution.
 * Though sites written in Java will sometimes produce java-level exceptions,
 * those exceptions are wrapped in a subclass of OrcException to localize and
 * isolate failures (see LocalException, JavaException).
 * 
 * @author dkitchin
 */
public class OrcException extends Exception {

	public OrcException(final String message) {
		super(message);
	}

	public OrcException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public OrcException(final Throwable cause) {
		super(cause);
	}
}
