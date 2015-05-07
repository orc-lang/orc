//
// FreeEvent.java -- Java class FreeEvent
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
import orc.trace.handles.LastHandle;
import orc.trace.values.ConstantValue;

/**
 * Dummy event used to free a handle to an event.
 * FIXME: I wish we could get rid of this. It has no
 * semantic value and is only necessary to support
 * correct memory management of handles.
 * 
 * @author quark
 */
public class FreeEvent extends Event {
	private final Handle<Event> event;

	public FreeEvent(final Event event) {
		this.event = new LastHandle<Event>(event);
	}

	@Override
	public void prettyPrintProperties(final Writer out, final int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "event", new ConstantValue(event.get()));
	}

	@Override
	public Term getProperty(final String key) {
		if (key.equals("event")) {
			return event.get();
		} else {
			return super.getProperty(key);
		}
	}

	@Override
	public String getType() {
		return "free";
	}

	@Override
	public <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
