//
// OtherwiseGroup.scala -- Scala class OtherwiseGroup
// Project OrcScala
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
class OtherwiseGroup(parent: Group, t: Token) extends Subgroup(parent) with Blocker {

  var state: OtherwiseGroupState = OtherwiseGroupState.LeftSideUnknown(t)

  t.blockOn(this)

  override def toString = super.toString + s"(state=${state.getClass().getSimpleName()}, alive=$isKilled)"

  override def publish(t: Token, v: Option[AnyRef]) {
    synchronized {
      state match {
        case OtherwiseGroupState.LeftSideUnknown(r) => { state = OtherwiseGroupState.LeftSidePublished; runtime.stage(r) }
        case OtherwiseGroupState.LeftSidePublished => { /* Left side publication is idempotent */ }
        case OtherwiseGroupState.LeftSideSilent => { throw new AssertionError("publication from silent f in f;g") }
      }
    }
    t.migrate(parent).publish(v)
  }

  override def onHalt() {
    synchronized {
      state match {
        case OtherwiseGroupState.LeftSideUnknown(r) => { state = OtherwiseGroupState.LeftSideSilent; runtime.stage(r) }
        case OtherwiseGroupState.LeftSidePublished => { /* Halting after publication does nothing */ }
        case OtherwiseGroupState.LeftSideSilent => { throw new AssertionError(s"halt of silent f in f;g: $this") }
      }
    }
    parent.remove(this)
  }

  def onDiscorporate() {
    synchronized {
      state match {
        case OtherwiseGroupState.LeftSideUnknown(r) => {
          // forcably discorporate the token we were holding on to.
          // The token has never run, but is also not truely halted, so discorporate don't kill.
          r.discorporate()
        }
        case OtherwiseGroupState.LeftSidePublished => { /* Halting after publication does nothing */ }
        case OtherwiseGroupState.LeftSideSilent => { throw new AssertionError("discorporate of silent f in f;g") }
      }
    }
    parent.discorporate(this)
  }

  override def check(t: Blockable): Unit = orc.util.Profiler.measureInterval(0L, 'OtherwiseGroup_check) {
    synchronized {
      state match {
        case OtherwiseGroupState.LeftSidePublished => { t.halt() }
        case OtherwiseGroupState.LeftSideSilent => { t.awake() }
        case OtherwiseGroupState.LeftSideUnknown(_) => { throw new AssertionError("Spurious check") }
      }
    }
  }

}

/** Possible states of an OtherwiseGroup */
abstract sealed class OtherwiseGroupState()

object OtherwiseGroupState {
  case class LeftSideUnknown(r: Token) extends OtherwiseGroupState()
  case object LeftSidePublished extends OtherwiseGroupState()
  case object LeftSideSilent extends OtherwiseGroupState()
}
