//
// SendEvent.java -- Java class SendEvent
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
import orc.trace.values.TupleValue;
import orc.trace.values.Value;

public class SendEvent extends Event {
	public final Value site;
	public final TupleValue arguments;

	public SendEvent(final Value site, final Value[] arguments) {
		this.site = site;
		this.arguments = new TupleValue(arguments);
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "site", site);
		prettyPrintProperty(out, indent, "arguments", arguments);
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("site")) {
			return site;
		} else if (key.equals("arguments")) {
			return arguments;
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "send";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
