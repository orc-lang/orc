//
// PublicationEvent.java -- Java class PublicationEvent
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

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.orchard.values.Value;

/**
 * Job publications (published Orc values).
 * @author quark
 */
@XmlSeeAlso(value = { Value.class })
public class PublicationEvent extends JobEvent {
	public Object value;

	public PublicationEvent() {
	}

	public PublicationEvent(final Object value) {
		this.value = value;
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
