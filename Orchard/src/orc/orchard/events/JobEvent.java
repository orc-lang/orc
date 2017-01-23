//
// JobEvent.java -- Java class JobEvent
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.events;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({ PublicationEvent.class, TokenErrorEvent.class, PrintlnEvent.class, PromptEvent.class, BrowseEvent.class })
public abstract class JobEvent implements Serializable {
    public int sequence;
    public Date timestamp;

    public abstract <E> E accept(Visitor<E> visit);
}
