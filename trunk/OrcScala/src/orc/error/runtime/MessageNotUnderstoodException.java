//
// MessageNotUnderstoodException.java -- Java class MessageNotUnderstoodException
// Project OrcJava
//
// $Id: MessageNotUnderstoodException.java 1502 2010-02-03 06:25:53Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.runtime;

public class MessageNotUnderstoodException extends TokenException {

	String field;

	public MessageNotUnderstoodException(final String field) {
		super("The message " + field + " was not understood by this site.");
		this.field = field;
	}

}
