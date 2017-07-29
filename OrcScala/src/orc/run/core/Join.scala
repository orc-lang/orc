//
// Join.scala -- Scala class Join
// Project OrcScala
//
// Created by dkitchin on Jan 22, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
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
abstract class JoinBase extends Blocker {
  val params: List[Binding]
  val waiter: Blockable
  val runtime: OrcRuntime

  val items = new Array[Binding](params.size)
  var state: JoinState = JoinState.InProgress(params.size)

  override def toString = super.toString + s"(state=$state, waiter=$waiter, ${params.mkString("[", ",", "]")}, ${items.mkString("[", ",", "]")})"

  def set(index: Int, arg: Binding) = synchronized {
    assert(items(index) == null)
    state match {
      case JoinState.InProgress(n) if (n > 0) => {
        items(index) = arg
        state = JoinState.InProgress(n - 1)
      }
      case JoinState.Halted => {}
      case _ => throw new AssertionError("Erroneous state transition in Join")
    }
    // If we just finished filling everything in then go to Complete state
    state match {
      case JoinState.InProgress(0) => {
        state = JoinState.Complete
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
      state = JoinState.Complete
      runtime.stage(waiter)
    }
  }

  def check(t: Blockable): Unit
}

class Join(val params: List[Binding], val waiter: Blockable, val runtime: OrcRuntime) extends JoinBase {
  def halt(index: Int) = synchronized {
    state match {
      case JoinState.InProgress(_) => {
        state = JoinState.Halted
        runtime.stage(waiter)
      }
      case JoinState.Halted => {}
      case JoinState.Complete => throw new AssertionError("Erroneous state transition in Join")
    }
  }

  def check(t: Blockable): Unit = orc.util.Profiler.measureInterval(0L, 'Join_check) {
    synchronized { state } match {
      case JoinState.InProgress(_) => throw new AssertionError(s"Spurious check on Join: $this")
      case JoinState.Halted => t.awakeStop()
      case JoinState.Complete =>
        val values = items.toList.map(_.asInstanceOf[BoundValue].v)
        t.awakeTerminalValue(values) // The checking entity must expect a list
    }
  }
}

class NonhaltingJoin(val params: List[Binding], val waiter: Blockable, val runtime: OrcRuntime) extends JoinBase {
  def halt(index: Int) = set(index, BoundStop)

  def check(t: Blockable): Unit = orc.util.Profiler.measureInterval(0L, 'NonhaltingJoin_check) {
    synchronized { state } match {
      case JoinState.InProgress(_) => throw new AssertionError("Spurious check on Join")
      case JoinState.Halted => throw new AssertionError("NonhaltingJoin halted")
      case JoinState.Complete => t.awakeTerminalValue(items.toList) // The checking entity must expect a List[Binding]
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

  def run(): Unit = orc.util.Profiler.measureInterval(0L, 'JoinItem_run) { obstacle foreach { _.check(this) } }

  override def unsetQuiescent() {
    source.waiter.unsetQuiescent()
  }
  override def setQuiescent() {
    source.waiter.setQuiescent()
  }
}

abstract sealed class JoinState()

object JoinState {
  case class InProgress(remaining: Int) extends JoinState()
  case object Complete extends JoinState()
  case object Halted extends JoinState()
}
