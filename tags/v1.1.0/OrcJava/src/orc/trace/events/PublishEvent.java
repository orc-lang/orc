//
// PublishEvent.java -- Java class PublishEvent
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
import orc.trace.values.Value;

/**
 * A top-level publication in a thread.
 */
public class PublishEvent extends Event {
	public final Value value;

	public PublishEvent(final Value value) {
		this.value = value;
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "value", value);
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("value")) {
			return value;
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "publish";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
