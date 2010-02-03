//
// SiteResolutionException.java -- Java class SiteResolutionException
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

package orc.error.compiletime;

/**
 * Indicate a problem with site resolution. Ideally
 * this would be a loadtime error, but currently site
 * resolution is done at runtime.
 * 
 * @author quark
 */
public class SiteResolutionException extends CompilationException {
	public SiteResolutionException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public SiteResolutionException(final String message) {
		super(message);
	}
}
