//
// Visitor.java -- Java interface Visitor
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

public interface Visitor<E> {
	public E visit(PrintlnEvent event);

	public E visit(PromptEvent event);

	public E visit(PublicationEvent event);

	public E visit(RedirectEvent event);

	public E visit(TokenErrorEvent event);
}
