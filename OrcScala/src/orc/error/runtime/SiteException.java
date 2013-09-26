//
// SiteException.java -- Java class SiteException
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

package orc.error.runtime;

/**
 * Any exception occurring in a well-formed, well-typed
 * site call. These are semantic exceptions from within
 * the site computation itself.
 *
 * @author dkitchin
 */
@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class SiteException extends TokenException {

	public SiteException(final String message) {
		super(message);
	}

	public SiteException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
