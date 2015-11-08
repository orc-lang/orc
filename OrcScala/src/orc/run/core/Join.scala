//
// Join.scala -- Scala class Join
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jan 22, 2013.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.OrcRuntime
import scala.reflect.ClassTag

/** A join point for waiting on multiple strict parameters.
  *
  * If any joined parameter halts, the join point halts.
  *
  * Joins are used to wait for all arguments to a site call,
  * since site calls are strict.
  *
  * @author dkitchin
  */
abstract class JoinBase extends Blocker {
  val params: List[Binding]
  val waiter: Blockable
  val runtime: OrcRuntime

  // TODO: Optimize the case where no parameter requires blocking.

  // Additional implementation will be needed to detect when a blocking step could be skipped,
  // for example, when a pruning group or closure is already bound.

  val items = new Array[Binding](params.size)
  var state: JoinState = JoinInProgress(params.size)

  override def toString = super.toString + s"(state=$state, waiter=$waiter, ${params.mkString("[", ",", "]")}, ${items.mkString("[", ",", "]")})"

  def set(index: Int, arg: Binding) = synchronized {
    assert(items(index) == null)
    state match {
      case JoinInProgress(n) if (n > 0) => {
        items(index) = arg
        state = JoinInProgress(n - 1)
      }
      case JoinHalted => {}
      case _ => throw new AssertionError("Erroneous state transformation in Join")
    }
    // If we just finished filling everything in then go to Complete state
    state match {
      case JoinInProgress(0) => {
        state = JoinComplete
        runtime.stage(waiter)
      }
      case _ => {}
    }
  }

  def halt(index: Int): Unit

  def join() = {
    waiter.blockOn(this)
    if (params.nonEmpty) {
      for ((param, i) <- params.view.zipWithIndex) {
        param match {
          case BoundValue(v) => set(i, param)
          case BoundStop => halt(i)
          case BoundReadable(g) => {
            val item = new JoinItem(this, i)
            g read item
          }
        }
      }
    } else {
      assert(params.isEmpty)
      state = JoinComplete
      runtime.stage(waiter)
    }
  }

  def check(t: Blockable): Unit
}

class Join(val params: List[Binding], val waiter: Blockable, val runtime: OrcRuntime) extends JoinBase {
  def halt(index: Int) = synchronized {
    state match {
      case JoinInProgress(_) => {
        state = JoinHalted
        runtime.stage(waiter)
      }
      case JoinHalted => {}
      case JoinComplete => throw new AssertionError("Erroneous state transformation in Join")
    }
  }

  def check(t: Blockable) = {
    synchronized { state } match {
      case JoinInProgress(_) => throw new AssertionError(s"Spurious check on Join: $this")
      case JoinHalted => t.awakeStop()
      case JoinComplete =>
        val values = items.toList.map(_.asInstanceOf[BoundValue].v)
        t.awakeTerminalValue(values) // The checking entity must expect a list
    }
  }
}

class NonhaltingJoin(val params: List[Binding], val waiter: Blockable, val runtime: OrcRuntime) extends JoinBase {
  def halt(index: Int) = set(index, BoundStop)

  def check(t: Blockable) = {
    synchronized { state } match {
      case JoinInProgress(_) => throw new AssertionError("Spurious check on Join")
      case JoinHalted => throw new AssertionError("NonhaltingJoin halted")
      case JoinComplete => t.awakeTerminalValue(items.toList) // The checking entity must expect a List[Binding]
    }
  }
}

class JoinItem(source: JoinBase, index: Int) extends Blockable {
  override val nonblocking = true

  var obstacle: Option[Blocker] = None

  def awakeNonterminalValue(v: AnyRef) = source.set(index, BoundValue(v))
  def awakeStop() = source.halt(index)
  override def halt() {} // Ignore halts

  def blockOn(b: Blocker) { obstacle = Some(b) }

  override def onSchedule() {
    unsetQuiescent()
  }

  override def onComplete() {
    setQuiescent()
  }

  def run() { obstacle foreach { _.check(this) } }

  override def unsetQuiescent() {
    source.waiter.unsetQuiescent()
  }
  override def setQuiescent() {
    source.waiter.setQuiescent()
  }
}

class JoinState
case class JoinInProgress(remaining: Int) extends JoinState
case object JoinComplete extends JoinState
case object JoinHalted extends JoinState
