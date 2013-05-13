//
// PruningGroup.scala -- Scala class PruningGroup
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.Schedulable

/** A PruningGroup is the group associated with expression g in (f <x< g).
  *
  * @author dkitchin
  */
class PruningGroup(parent: Group) extends Subgroup(parent) with Blocker {

  var state: PruningGroupState = RightSideUnknown(Nil)

  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    state match {
      case RightSideUnknown(waitlist) => {
        state = RightSidePublished(v)
        t.kill()
        this.kill()
        for (w <- waitlist) { runtime.stage(w) }
      }
      case _ => t.kill()
    }
  }

  def onHalt() = synchronized {
    state match {
      case RightSideUnknown(waitlist) => {
        state = RightSideSilent
        parent.remove(this)
        for (w <- waitlist) { runtime.stage(w) }
      }
      case _ => {}
    }
  }

  // Specific to PruningGroups
  def read(t: Blockable) = synchronized {
    state match {
      case RightSidePublished(v) => t.awakeValue(v.get) // t.publish(Some(v))
      case RightSideSilent => t.awakeStop() // t.publish(None)
      case RightSideUnknown(waitlist) => {
        t.blockOn(this)
        state = RightSideUnknown(t :: waitlist)
      }
    }
  }

  def check(t: Blockable) {
    synchronized {
      state match {
        case RightSidePublished(v) => t.awakeValue(v.get) // t.publish(Some(v))
        case RightSideSilent => t.awakeStop() // t.publish(None)
        case RightSideUnknown(_) => { throw new AssertionError("Spurious check") }
      }
    }
  }

}

/** Possible states of a PruningGroup */
class PruningGroupState
case class RightSideUnknown(waitlist: List[Schedulable]) extends PruningGroupState
case class RightSidePublished(v: Option[AnyRef]) extends PruningGroupState
case object RightSideSilent extends PruningGroupState
