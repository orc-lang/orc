//
// PrintEvent.java -- Java class PrintEvent
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

import orc.trace.Term;
import orc.trace.values.ConstantValue;

/**
 * Printing to stdout.
 * @author quark
 */
public class PrintEvent extends Event {
	public final String output;
	public final boolean newline;

	public PrintEvent(final String output, final boolean newline) {
		this.output = output;
		this.newline = newline;
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "output", new ConstantValue(output));
		if (newline) {
			prettyPrintProperty(out, indent, "newline", new ConstantValue(true));
		}
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("output")) {
			return new ConstantValue(output);
		}
		if (key.equals("newline")) {
			return new ConstantValue(newline);
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "print";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
