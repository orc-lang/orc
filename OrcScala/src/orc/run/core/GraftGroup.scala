//
// GraftGroup.scala -- Scala class GraftGroup
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

/** A GraftGroup is the group associated with expression g in val x = g # f.
  *
  * We use early initialization here because the group can be killed as soon as the
  * constructor for Subgroup runs. So normal initialization could be too late and result
  * in crashes in the kill call.
  *
  * @author dkitchin, amp
  */
class GraftGroup(parent: Group) extends {
  var state: GraftGroupState = GraftGroupState.ValueUnknown
  private var _future: Future = new LocalFuture(parent.runtime)
} with Subgroup(parent) {
  override def toString = super.toString + s"(state=${state}, ${_future})"

  /** Get a binding connecting to this graft group.
    *
    * This should only be called before any member of this group can run. This
    * is because if this group has bound the future by publication then this
    * method will not work. So binding should only be called shortly after
    * construction before anything has been scheduled (staging is OK as long
    * as the stage has not been flushed by returning to the scheduler).
    *
    * This will usually return a BoundReadable, however if the group is silent
    * (due to halting or being killed) this will return a BoundStop.
    */
  def binding = synchronized {
    if (_future ne null)
      BoundReadable(_future)
    else if (state == GraftGroupState.ValueSilent)
      BoundStop
    else
      throw new AssertionError(s"Requesting binding for bound graft group. This should not be possible. This must be a threading issue. $this")
  }

  // Publishing is idempotent
  override def publish(t: Token, v: Option[AnyRef]) = synchronized {
    state match {
      case GraftGroupState.ValueUnknown => {
        state = GraftGroupState.ValuePublished
        // There should be no situations in which v is None. Just let it crash if it's not.
        _future.bind(v.get)
        // Clear the reference to the future so that it can be collected even if this group still exists.
        _future = null
      }
      case _ => {}
    }

    t.halt()
  }

  override def onHalt() = synchronized {
    state match {
      case GraftGroupState.ValueUnknown => {
        state = GraftGroupState.ValueSilent
        parent.remove(this)
        _future.stop()
        _future = null
      }
      case GraftGroupState.ValuePublished => {
        parent.remove(this)
      }
      case _ => {}
    }
  }

  def onDiscorporate() = synchronized {
    state match {
      case GraftGroupState.ValueUnknown => {
        parent.discorporate(this)
        _future = null
      }
      case GraftGroupState.ValuePublished => {
        parent.discorporate(this)
      }
      case _ => {}
    }
  }

  // This is not needed for Graft itself. However it doesn't hurt anything and it is needed for
  // object field futures to halt when the object is killed.
  override def kill() = {
    synchronized {
      state match {
        case GraftGroupState.ValueUnknown => {
          state = GraftGroupState.ValueSilent
          _future.stop()
          _future = null
        }
        case _ => {}
      }
    }
    super.kill()
  }
}

/** Possible states of a GraftGroup */
abstract sealed class GraftGroupState()
object GraftGroupState {
  case object ValueUnknown extends GraftGroupState()
  case object ValuePublished extends GraftGroupState()
  case object ValueSilent extends GraftGroupState()
}
