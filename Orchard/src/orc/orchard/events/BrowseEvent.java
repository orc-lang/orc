//
// BrowseEvent.java -- Java class BrowseEvent
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

import java.net.URL;

public class BrowseEvent extends JobEvent {
    private static final long serialVersionUID = 3611166309194063396L;
    public URL url;

    public BrowseEvent() {
    }

    public BrowseEvent(final URL url) {
        this.url = url;
    }

    @Override
    public <E> E accept(final Visitor<E> visitor) {
        return visitor.visit(this);
    }
}
