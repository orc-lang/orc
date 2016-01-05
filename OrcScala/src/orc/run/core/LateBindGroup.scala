//
// PruningGroup.scala -- Scala class PruningGroup
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.Schedulable

/** A LateBindGroup is the group associated with expression g in (f <x<| g).
  *
  * @author dkitchin, amp
  */
class LateBindGroup(parent: Group) extends Subgroup(parent) with Blocker {

  var state: LateBindGroupState = RightSideUnknown(Nil)

  override def toString = super.toString + s"(state=${state.getClass().getSimpleName()})"

  // Publishing is idempotent
  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    state match {
      case RightSideUnknown(waitlist) => {
        state = RightSidePublished(v)
        for (w <- waitlist) { runtime.stage(w) }
      }
      case _ => {}
    }
    
    t.halt()
  }

  def onHalt() = synchronized {
    state match {
      case RightSideUnknown(waitlist) => {
        state = RightSideSilent
        parent.remove(this)
        for (w <- waitlist) { runtime.stage(w) }
      }
      case RightSidePublished(_) => {
        parent.remove(this)
      }
      case _ => {}
    }
  }

  // Specific to PruningGroups
  def read(t: Blockable) = {
    // result encodes what calls to t.awake* should be made after releasing the lock
    val result = synchronized {
      state match {
        case RightSidePublished(v) => Some(v)
        case RightSideSilent => Some(None)
        case RightSideUnknown(waitlist) => {
          t.blockOn(this)
          state = RightSideUnknown(t :: waitlist)
          None
        }
      }
    }

    result match {
      case Some(Some(v)) => t.awakeValue(v)
      case Some(None) => t.awakeStop()
      case None => {}
    }
  }

  def check(t: Blockable) {
    synchronized { state } match {
      case RightSidePublished(v) => t.awakeValue(v.get)
      case RightSideSilent => t.awakeStop()
      case RightSideUnknown(_) => { throw new AssertionError("Spurious check") }
    }
  }

}

/** Possible states of a PruningGroup */
sealed abstract class LateBindGroupState
case class RightSideUnknown(waitlist: List[Schedulable]) extends LateBindGroupState
case class RightSidePublished(v: Option[AnyRef]) extends LateBindGroupState
case object RightSideSilent extends LateBindGroupState
