//
// NullValue.java -- Java class NullValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.values;

import java.io.IOException;
import java.io.Writer;

public class NullValue extends AbstractValue {
	public final static NullValue singleton = new NullValue();

	private NullValue() {
	}

	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write("Null()");
	}
}
