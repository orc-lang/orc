//
// BlockEvent.java -- Java class BlockEvent
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
import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;

/**
 * Thread is blocked waiting for a Future.
 * 
 * @author quark
 */
public class BlockEvent extends Event {
	public Handle<PullEvent> pull;

	public BlockEvent(final PullEvent pull) {
		this.pull = new RepeatHandle<PullEvent>(pull);
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "pull", new ConstantValue(pull.get()));
	}

	@Override
	public String getType() {
		return "block";
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("pull")) {
			return pull.get();
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
