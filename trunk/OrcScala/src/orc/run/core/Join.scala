//
// Join.scala -- Scala class Join
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 22, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.OrcRuntime

/** A join point for waiting on multiple strict parameters.
  *
  * If any joined parameter halts, the join point halts.
  *
  * Joins are used to wait for all arguments to a site call,
  * since site calls are strict.
  *
  * @author dkitchin
  */
class Join(params: List[Binding], t: Blockable, val runtime: OrcRuntime) extends Blocker {

  // TODO: Optimize the case where no parameter requires blocking.

  // Additional implementation will be needed to detect when a blocking step could be skipped,
  // for example, when a pruning group or closure is already bound.

  val items = new Array[AnyRef](params.size)
  var state: JoinState = JoinInProgress(params.size)

  def set(index: Int, arg: AnyRef) = synchronized {
    state match {
      case JoinInProgress(n) if (n > 1) => {
        items(index) = arg
        state = JoinInProgress(n - 1)
      }
      case JoinInProgress(1) => {
        items(index) = arg
        state = JoinComplete
        runtime.stage(t)
      }
      case JoinHalted => {}
      case _ => throw new AssertionError("Erroneous state transformation in Join")
    }
  }

  def halt() = synchronized {
    state match {
      case JoinInProgress(_) => {
        state = JoinHalted
        runtime.stage(t)
      }
      case JoinHalted => {}
      case JoinComplete => throw new AssertionError("Erroneous state transformation in Join")
    }
  }

  def join() = {
    t.blockOn(this)
    for ((param, i) <- params.view.zipWithIndex) {
      param match {
        case BoundValue(v) => set(i, v)
        case BoundStop => halt()
        case BoundFuture(g) => {
          val item = new JoinItem(this, i)
          g read item
        }
        case BoundClosure(c) => {
          val item = new JoinItem(this, i)
          c read item
        }
      }
    }
  }

  def check(t: Blockable) = synchronized {
    state match {
      case JoinInProgress(_) => throw new AssertionError("Spurious check on Join")
      case JoinHalted => t.awakeStop()
      case JoinComplete => t.awakeValue(items.toList) // The checking entity must expect a list
    }
  }

}

class JoinItem(source: Join, index: Int) extends Blockable {

  var obstacle: Option[Blocker] = None

  def awakeValue(v: AnyRef) = source.set(index, v)
  def awakeStop() = source.halt()

  def blockOn(b: Blocker) { obstacle = Some(b) }

  def run() { obstacle foreach { _.check(this) } }

}

class JoinState
case class JoinInProgress(remaining: Int) extends JoinState
case object JoinComplete extends JoinState
case object JoinHalted extends JoinState
