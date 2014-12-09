//
// Closure.scala -- Scala class Closure
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

import orc.ast.oil.nameless.Def
import orc.Schedulable
import orc.OrcRuntime
import orc.util.BlockableMapExtension

/** A closure that both resolves itself and represents the closure itself. This should
  * be scheduled when it is created.
  *
  * @author dkitchin, amp
  */
class Closure(
  index: Int,
  val closureGroup: ClosureGroup)
  extends Blocker {

  def defs = closureGroup._defs

  def code: Def = defs(index)

  def context = closureGroup.context

  def lexicalContext = closureGroup.lexicalContext

  def check(t: Blockable) = closureGroup.check(t, index)

  def read(t: Blockable) = closureGroup.read(t, index)

  override val runtime = closureGroup.runtime

  override def toString = super.toString + (code.body.pos, closureGroup, index)
}

class ClosureGroup(
  private[run] var _defs: List[Def],
  private var _lexicalContext: List[Binding],
  val runtime: OrcRuntime)
  extends Schedulable
  with Blockable {
  import ClosureGroup._
  import BlockableMapExtension._
  
  def defs = _defs
  def lexicalContext = _lexicalContext
  
  private val toStringRecusionGuard = new ThreadLocal[Object]()
  override def toString = {
    try {
      val recursing = toStringRecusionGuard.get
      toStringRecusionGuard.set(java.lang.Boolean.TRUE)
      super.toString.stripPrefix("orc.run.core.") + (if (recursing eq null) s"(state=${state})" else "")
    } finally {
      toStringRecusionGuard.remove()
    }
  }

  /** Create all the closures. They forward most of their methods here.
    */
  val closures = defs.indices.toList map { i => new Closure(i, this) }

  /** Stores the current version of the context. The initial value has BoundClosures for each closure,
    * so they will still be resolved if this context is used.
    */
  private var _context: List[Binding] = closures.reverse.map { BoundClosure(_) } ::: lexicalContext

  /** Get the context used by all of the closures in this group.
    */
  def context = _context
  /* 
   * This should be safe without locking because the change is still atomic (pointer assignment)
   * and getting the old version doesn't actually hurt anything. It just slows down the 
   * evaluation of the token that got it because it will have to resolve the future versions
   * of things.
   */

  // State is used for the blocker side
  private var state: ClosureState = Started

  // waitlist is effectively the state of the blocker side.
  private var waitlist: List[Blockable] = Nil // This should be empty at any time state = Resolved

  private var activeCount = 0

  override def setQuiescent(): Unit = synchronized {
    assert(activeCount > 0)
    activeCount -= 1
    if (activeCount == 0) {
      waitlist foreach { _.setQuiescent() }
    }
  }
  override def unsetQuiescent(): Unit = synchronized {
    if (activeCount == 0) {
      waitlist foreach { _.unsetQuiescent() }
    }
    activeCount += 1
    assert(activeCount > 0)
  }

  /** Execute the resolution process of this Closure. This should be called by the scheduler.
    */
  def run() = synchronized {
    state match {
      case Started => {
        // Start the resolution process
        val join = new NonhaltingJoin(_lexicalContext, this, runtime)
        join.join()
      }
      case Blocked(b) => {
        b.check(this)
      }
      case Resolved => throw new AssertionError("Closure scheduled in bad state: " + state)
    }
  }

  //// Blocker Implementation

  def check(t: Blockable, i: Int) = {
    synchronized { state } match {
      case Resolved => 
        t.awakeTerminalValue(closures(i))
      case _ => throw new AssertionError("Closure.check called in bad state: " + state)
    }
  }

  def read(t: Blockable, i: Int) = {
    val doawake = synchronized {
      state match {
        case Resolved => true
        case _ => {
          t.blockOn(closures(i))
          waitlist ::= t
          if (activeCount > 0) {
            t.unsetQuiescent()
          }
          false
        }
      }
    }

    if (doawake) {
      t.awakeTerminalValue(closures(i))
    }
  }

  //// Blockable Implementation

  def awakeNonterminalValue(v: AnyRef) = {
    // We only block on things that produce a single value so we don't have to handle multiple awakeNonterminalValue calls from one source.
    val bindings = v.asInstanceOf[List[Binding]]
    synchronized {
      _lexicalContext = bindings
      _context = closures.reverse.map { BoundValue(_) } ::: lexicalContext
      state = Resolved
      waitlist foreach runtime.stage
      //waitlist = Nil
    }
  }
  def awakeStop() = throw new AssertionError("Should never halt")
  override def halt() {} // Ignore halts

  def blockOn(b: Blocker) {
    assert(state != Resolved)
    state = Blocked(b)
  }

  //// Schedulable Implementation to handle Vclock correctly

  // We cannot just unset/set on all elements of waitlist because that structure may change at any time.
  override def onSchedule() {
    unsetQuiescent()
  }

  override def onComplete() {
    setQuiescent()
  }
}

object ClosureGroup {
  sealed trait ClosureState
  case class Blocked(blocker: Blocker) extends ClosureState
  case object Resolved extends ClosureState
  case object Started extends ClosureState
}
