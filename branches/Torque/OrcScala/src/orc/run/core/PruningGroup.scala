//
// PruningGroup.scala -- Scala class/trait/object PruningGroup
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

/** Possible states of a PruningGroup */
class PruningGroupState
case class Unbound(waitlist: List[Token]) extends PruningGroupState
case class Bound(v: AnyRef) extends PruningGroupState
case object Dead extends PruningGroupState


/** A PruningGroup is the group associated with expression g in (f <x< g) */
class PruningGroup(parent: Group) extends Subgroup(parent) with Blocker {

  val quiescentWhileBlocked = true
  
  var state: PruningGroupState = Unbound(Nil)

  def publish(t: Token, v: AnyRef) = synchronized {
    state match {
      case Unbound(waitlist) => {
        state = Bound(v)
        t.halt()
        this.kill()
        for (t <- waitlist) { t.publish(Some(v)) }
      }
      case _ => t.halt()
    }
  }

  def onHalt() = synchronized {
    state match {
      case Unbound(waitlist) => {
        state = Dead
        parent.remove(this)
        for (t <- waitlist) { t.publish(None) }
      }
      case _ => {}
    }
  }

  // Specific to PruningGroups
  def read(t: Token) = synchronized {
    state match {
      case Bound(v) => t.publish(Some(v))
      case Dead => t.publish(None)
      case Unbound(waitlist) => {
        t.blockOn(this)
        state = Unbound(t :: waitlist)
      }
    }
  }

}