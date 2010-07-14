//
// MessageNotUnderstoodException.java -- Java class MessageNotUnderstoodException
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

@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public class MessageNotUnderstoodException extends TokenException {

	public String field;

	public MessageNotUnderstoodException(@SuppressWarnings("hiding") final String field, final String siteName) {
		super("The message '" + field + "' was not understood by the " + siteName + " site.");
		this.field = field;
	}

}
