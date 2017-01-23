//
// Closure.scala -- Scala class ResolvableCollection
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

import orc.{ OrcRuntime, Schedulable }

/** An member of a collection of Ts that need a single resolved recursive context.
  *
  * @author amp
  */
class ResolvableCollectionMember[T](
  index: Int,
  val collection: ResolvableCollection[T, ResolvableCollectionMember[T]])
  extends ReadableBlocker {

  def definition: T = collection.definitions(index)

  def context = collection.context

  def lexicalContext = collection.lexicalContext

  override def check(t: Blockable) = collection.check(t, index)

  def read(t: Blockable) = collection.read(t, index)
}

/** A collection of Ts that all need a shared recursive context. This will
  * resolve that context and should be scheduled to allow resolution.
  *
  * @author dkitchin, amp
  */
abstract class ResolvableCollection[T, +Member <: ResolvableCollectionMember[T]](
  private[run] var _defs: List[T],
  private var _lexicalContext: List[Binding],
  val runtime: OrcRuntime)
  extends Schedulable
  with Blockable {
  import ResolvableCollection._

  /** Execution of a ResolvableCollection cannot indefinitely block the executing thread. */
  override val nonblocking = true

  def definitions = _defs
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

  def buildMember(i: Int): Member

  /** Create all the closures. They forward most of their methods here.
    */
  val members = definitions.indices.toList map buildMember

  /** Stores the current version of the context. The initial value has BoundClosures for each closure,
    * so they will still be resolved if this context is used.
    */
  private var _context: List[Binding] = members.reverse.map { BoundReadable(_) } ::: lexicalContext

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
  private var state: ResolvableCollectionState = Started

  // Waitlist is the state of the blocker side. 
  // These objects are below this in the dynamic scoping and hence cannot be called with the this lock held.
  private var waitlist: List[Blockable] = Nil
  // This should be empty at any time state = Resolved

  private var activeCount = 0

  override def setQuiescent(): Unit = {
    val blockablesToSet = synchronized {
      assert(activeCount > 0)
      activeCount -= 1
      if (activeCount == 0) waitlist else Nil
    }
    blockablesToSet foreach { _.setQuiescent() }
  }
  override def unsetQuiescent(): Unit = {
    val blockablesToUnset = synchronized {
      assert(activeCount >= 0)
      activeCount += 1
      // If activeCount WAS 0
      if (activeCount == 1) waitlist else Nil
    }
    blockablesToUnset foreach { _.unsetQuiescent() }
  }

  /** Execute the resolution process of this Closure. This should be called by the scheduler.
    */
  override def run() = {
    // This synchronized may not be needed since we only change state from within this method which is 
    // only called when this is scheduled which can only happen once (from the join).
    synchronized { state } match {
      case Started => {
        // We can safely read _lexicalContext without a lock because it is only written from the Blocked 
        // state which we cannot reach until after the join is started.
        val join = new NonhaltingJoin(_lexicalContext, this, runtime)
        join.join()
      }
      case Blocked(b) => {
        b.check(this)
      }
      case Resolved => throw new AssertionError("Closure scheduled in bad state: " + state)
    }
  }

  def isResolved = state match {
    case Resolved => true
    case _ => false
  }

  //// Blocker Implementation

  def check(t: Blockable, i: Int) = {
    synchronized { state } match {
      case Resolved =>
        t.awakeTerminalValue(members(i))
      case _ => throw new AssertionError("Closure.check called in bad state: " + state)
    }
  }

  def read(t: Blockable, i: Int) = {
    // To avoid a negative transient of the activeCount of t (to zero being the problem)
    // we unset here and then set again to undo it after if it was not needed. This means that
    // blockables in waitlist can always be set without breaking things.
    // t is guarenteed not to be quiescent (since it is calling read), so unsetting it again
    // for a little while will not cause an extra step to 0. So we are intensionally creating
    // a positive transient to avoid a possible negative transient.
    t.unsetQuiescent()

    val (doawake, doset) = synchronized {
      state match {
        case Resolved => (true, true)
        case _ => {
          t.blockOn(members(i))
          waitlist ::= t
          (false, activeCount == 0)
        }
      }
    }

    // Now undo the unset from above if needed.
    if (doset) {
      t.setQuiescent()
    }

    if (doawake) {
      t.awakeTerminalValue(members(i))
    }
  }

  //// Blockable Implementation

  override def awakeNonterminalValue(v: AnyRef) = {
    // We only block on things that produce a single value so we don't have to handle multiple awakeNonterminalValue calls from one source.
    val bindings = v.asInstanceOf[List[Binding]]
    val waits = synchronized {
      _lexicalContext = bindings
      _context = members.reverse.map { BoundValue(_) } ::: lexicalContext
      state = Resolved
      waitlist
      //waitlist = Nil
    }
    waits foreach runtime.stage
  }
  override def awakeStop() = throw new AssertionError("Should never halt")
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

object ResolvableCollection {
  sealed abstract class ResolvableCollectionState
  case class Blocked(blocker: Blocker) extends ResolvableCollectionState
  case object Resolved extends ResolvableCollectionState
  case object Started extends ResolvableCollectionState
}
