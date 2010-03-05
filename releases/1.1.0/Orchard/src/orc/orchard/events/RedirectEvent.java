//
// RedirectEvent.java -- Java class RedirectEvent
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

import java.net.URL;

public class RedirectEvent extends JobEvent {
	public URL url;

	public RedirectEvent() {
	}

	public RedirectEvent(final URL url) {
		this.url = url;
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
