//
// StoreEvent.java -- Java class StoreEvent
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
import orc.trace.TokenTracer.StoreTrace;
import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;

/**
 * Store a value to a future. Should be followed by a {@link FreeEvent} which
 * indicates that all the effects of setting the future have been recorded.
 * 
 * @author quark
 */
public class StoreEvent extends Event implements StoreTrace {
	public Value value;
	public Handle<PullEvent> pull;

	public StoreEvent(final PullEvent pull, final Value value) {
		this.pull = new RepeatHandle<PullEvent>(pull);
		this.value = value;
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "pull", new ConstantValue(pull.get()));
		prettyPrintProperty(out, indent, "value", value);
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("pull")) {
			return pull.get();
		}
		if (key.equals("value")) {
			return value;
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "store";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
