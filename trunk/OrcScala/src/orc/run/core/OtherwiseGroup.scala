//
// OtherwiseGroup.scala -- Scala class OtherwiseGroup
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

/** An OtherwiseGroup is the group associated with expression f in (f ; g)
  *
  * @author dkitchin
  */
class OtherwiseGroup(parent: Group, t: Blockable) extends Subgroup(parent) with Blocker {

  var state: OtherwiseGroupState = LeftSideUnknown(t)

  t.blockOn(this)

  def publish(t: Token, v: Option[AnyRef]) {
    synchronized {
      state match {
        case LeftSideUnknown(r) => { state = LeftSidePublished; runtime.stage(r) }
        case LeftSidePublished => { /* Left side publication is idempotent */ }
        case LeftSideSilent => { throw new AssertionError("publication from silent f in f;g") }
      }
    }
    t.migrate(parent).publish(v)
  }

  def onHalt() {
    synchronized {
      state match {
        case LeftSideUnknown(r) => { state = LeftSideSilent; runtime.stage(r) }
        case LeftSidePublished => { /* Halting after publication does nothing */ }
        case LeftSideSilent => { throw new AssertionError("halt of silent f in f;g") }
      }
    }
    parent.remove(this)
  }

  def check(t: Blockable) {
    synchronized {
      state match {
        case LeftSidePublished => { t.halt() } // t.halt()
        case LeftSideSilent => { t.awake() } // t.unblock()
        case LeftSideUnknown(_) => { throw new AssertionError("Spurious check") }
      }
    }
  }

}

/** Possible states of an OtherwiseGroup */
class OtherwiseGroupState
case class LeftSideUnknown(r: Blockable) extends OtherwiseGroupState
case object LeftSidePublished extends OtherwiseGroupState
case object LeftSideSilent extends OtherwiseGroupState
