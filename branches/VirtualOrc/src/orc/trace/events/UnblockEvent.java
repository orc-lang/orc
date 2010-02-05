//
// UnblockEvent.java -- Java class UnblockEvent
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
 * Resume after a {@link BlockEvent}.
 * @author quark
 */
public class UnblockEvent extends Event {
	public Handle<StoreEvent> store;

	public UnblockEvent(final StoreEvent store) {
		this.store = new RepeatHandle<StoreEvent>(store);
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "store", new ConstantValue(store.get()));
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("store")) {
			return store.get();
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "unblock";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
