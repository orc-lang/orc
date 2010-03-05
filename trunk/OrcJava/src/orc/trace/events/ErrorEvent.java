//
// ErrorEvent.java -- Java class ErrorEvent
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

package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.error.runtime.TokenException;
import orc.trace.Term;
import orc.trace.values.ConstantValue;

/**
 * A fatal error in a thread.
 */
public class ErrorEvent extends Event {
	public final TokenException error;

	public ErrorEvent(final TokenException error) {
		this.error = error;
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "errorMessage", new ConstantValue(error.getMessage()));
		prettyPrintProperty(out, indent, "errorType", new ConstantValue(error.getClass().getName()));
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("errorMessage")) {
			return new ConstantValue(error.getMessage());
		}
		if (key.equals("errorType")) {
			return new ConstantValue(error.getClass().getName());
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "error";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
