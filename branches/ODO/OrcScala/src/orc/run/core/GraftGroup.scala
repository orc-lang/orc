//
// GraftGroup.scala -- Scala class PruningGroup
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

/** A GraftGroup is the group associated with expression g in val x = g # f.
  *
  * @author dkitchin, amp
  */
class GraftGroup(parent: Group) extends Subgroup(parent) with Blocker {

  var state: GraftGroupState = ValueUnknown(Nil)

  override def toString = super.toString + s"(state=${state.getClass().getSimpleName()})"

  // Publishing is idempotent
  def publish(t: Token, v: Option[AnyRef]) = synchronized {
    state match {
      case ValueUnknown(waitlist) => {
        state = ValuePublished(v)
        for (w <- waitlist) { runtime.stage(w) }
      }
      case _ => {}
    }
    
    t.halt()
  }

  def onHalt() = synchronized {
    state match {
      case ValueUnknown(waitlist) => {
        state = ValueSilent
        parent.remove(this)
        for (w <- waitlist) { runtime.stage(w) }
      }
      case ValuePublished(_) => {
        parent.remove(this)
      }
      case _ => {}
    }
  }

  // Specific to GraftGroups
  def read(t: Blockable) = {
    // result encodes what calls to t.awake* should be made after releasing the lock
    val result = synchronized {
      state match {
        case ValuePublished(v) => Some(v)
        case ValueSilent => Some(None)
        case ValueUnknown(waitlist) => {
          t.blockOn(this)
          state = ValueUnknown(t :: waitlist)
          None
        }
      }
    }

    result match {
      case Some(Some(v)) => t.awakeTerminalValue(v)
      case Some(None) => t.awakeStop()
      case None => {}
    }
  }

  def check(t: Blockable) {
    synchronized { state } match {
      case ValuePublished(v) => t.awakeTerminalValue(v.get)
      case ValueSilent => t.awakeStop()
      case ValueUnknown(_) => { throw new AssertionError("Spurious check") }
    }
  }

}

/** Possible states of a PruningGroup */
class GraftGroupState
case class ValueUnknown(waitlist: List[Schedulable]) extends GraftGroupState
case class ValuePublished(v: Option[AnyRef]) extends GraftGroupState
case object ValueSilent extends GraftGroupState
