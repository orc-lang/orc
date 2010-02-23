//
// EnvException.java -- Java class EnvException
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

package orc.env;

import orc.error.OrcException;

/**
 * 
 * @author dkitchin
 */
public class EnvException extends OrcException {

	public EnvException(final String message) {
		super(message);
	}

	public EnvException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
