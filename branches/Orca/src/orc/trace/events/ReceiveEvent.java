//
// ReceiveEvent.java -- Java class ReceiveEvent
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
import orc.trace.values.Value;

/**
 * Return from a site call. At one point this had a handle to the corresponding
 * site call, but that's not really necessary (the call is just the preceeding
 * {@link SendEvent} in the same thread) and we may not want to bother recording
 * the call (which should be deterministic anyways).
 * 
 * @author quark
 */
public class ReceiveEvent extends Event {
	public final Value value;
	public final int latency;

	public ReceiveEvent(final Value value) {
		// use a dummy value of -1 for unknown latency
		this(value, -1);
	}

	public ReceiveEvent(final Value value, final int latency) {
		this.value = value;
		this.latency = latency;
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "value", value);
		prettyPrintProperty(out, indent, "latency", new ConstantValue(latency));
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("value")) {
			return value;
		}
		if (key.equals("latency")) {
			return new ConstantValue(latency);
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "receive";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
