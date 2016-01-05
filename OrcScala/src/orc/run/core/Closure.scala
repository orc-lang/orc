//
// Closure.scala -- Scala class Closure
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
  with Blockable with Resolver {
  import ClosureGroup._
  import BlockableMapExtension._

  def defs = _defs
  def lexicalContext = _lexicalContext

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

  private var stack: List[Option[AnyRef] => Unit] = Nil

  private def pop() = {
    val top = stack.head
    stack = stack.tail
    top
  }
  private def push(k: (Option[AnyRef] => Unit)) = stack = k :: stack
  protected def pushContinuation(k: (Option[AnyRef] => Unit)) = push(k)

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

  /** Create a new Closure object whose lexical bindings are all resolved and replaced.
    * Such a closure will have no references to any group.
    * This object is then passed to the continuation.
    */
  private def enclose(bs: List[Binding])(k: List[Binding] => Unit) {
    def resolveBound(b: Binding)(k: Binding => Unit) =
      resolveOptional(b) {
        case Some(v) => k(BoundValue(v))
        case None => k(BoundStop)
      }
    bs.blockableMap(resolveBound)(k)
  }

  /** Execute the resolution process of this Closure. This should be called by the scheduler.
    */
  def run() = synchronized {
    state match {
      case Started => {
        // Start the resolution process
        enclose(_lexicalContext) { newContext =>
          // Have to synchronize again because this is really 
          // a callback that will be called some time later.
          synchronized {
            assert(stack.size == 0) // Check for a bug that existed before and was non-deterministic
            _lexicalContext = newContext
            _context = closures.reverse.map { BoundValue(_) } ::: lexicalContext
            state = Resolved
            waitlist foreach runtime.stage
            //waitlist = Nil
          }
        }
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
      case Resolved => t.awakeValue(closures(i))
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

    if (doawake)
      t.awakeValue(closures(i))
  }

  //// Blockable Implementation

  private def handleValue(v: Option[AnyRef]) {
    pop()(v) // Pop and call the old top of the stack on the value. The pop happens first. That's important.
  }

  def awakeValue(v: AnyRef) = handleValue(Some(v))
  def awakeStop() = handleValue(None)

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
  sealed abstract class ClosureState
  case class Blocked(blocker: Blocker) extends ClosureState
  case object Resolved extends ClosureState
  case object Started extends ClosureState
}
