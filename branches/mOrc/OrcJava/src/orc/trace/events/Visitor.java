//
// Visitor.java -- Java interface Visitor
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

/**
 * Used to perform a type-case on events.
 * @author quark
 *
 * @param <V> return type of visitor
 */
public interface Visitor<V> {
	public V visit(BlockEvent event);

	public V visit(ChokeEvent event);

	public V visit(DieEvent event);

	public V visit(ErrorEvent event);

	public V visit(ForkEvent event);

	public V visit(FreeEvent event);

	public V visit(PrintEvent event);

	public V visit(PublishEvent event);

	public V visit(PullEvent event);

	public V visit(ReceiveEvent event);

	public V visit(SendEvent event);

	public V visit(StoreEvent event);

	public V visit(UnblockEvent event);
}
