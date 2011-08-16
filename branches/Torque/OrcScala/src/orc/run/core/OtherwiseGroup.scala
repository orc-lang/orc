//
// OtherwiseGroup.scala -- Scala class/trait/object OtherwiseGroup
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

/**
 * 
 *
 * @author dkitchin
 */
/** A OtherwiseGroup is the group associated with expression f in (f ; g) */
class OtherwiseGroup(parent: Group, t: Token) extends Subgroup(parent) with Blocker {

  /* Some(t): No publications have left this group.
   *          If the group halts silently, t will be scheduled.
   *    None: One or more publications has left this group.
   */
  var pending: Option[Token] = Some(t)
  
  val quiescentWhileBlocked = true
  
  t.blockOn(this)

  def publish(t: Token, v: AnyRef) = synchronized {
    pending foreach { _.halt() } // Remove t from its group
    pending = None
    t.migrate(parent).publish(v)
  }

  def onHalt() = synchronized {
    pending foreach { _.unblock }
    parent.remove(this)
  }

}