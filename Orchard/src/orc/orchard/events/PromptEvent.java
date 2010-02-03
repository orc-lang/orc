//
// PromptEvent.java -- Java class PromptEvent
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

public class PromptEvent extends JobEvent {
	public String message;
	public int promptID;

	public PromptEvent() {
	}

	public PromptEvent(final int promptID, final String message) {
		this.promptID = promptID;
		this.message = message;
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
