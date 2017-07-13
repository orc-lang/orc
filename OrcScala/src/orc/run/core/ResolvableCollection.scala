//
// ResolvableCollection.scala -- Scala class ResolvableCollection
// Project OrcScala
//
// Created by amp on Jan 15, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

import orc.{ OrcRuntime, Schedulable }
import orc.run.core.VirtualClock

/** An member of a collection of Ts that need a single resolved recursive context.
  *
  * @author amp
  */
class ResolvableCollectionMember[T](
  index: Int,
  val collection: ResolvableCollection[T, ResolvableCollectionMember[T]]) {

  def definition: T = collection.definitions(index)

  def context = collection.context

  def lexicalContext = collection.lexicalContext
}

/** A collection of Ts that all need a shared recursive context. This will
  * resolve that context and should be scheduled to allow resolution.
  *
  * @author dkitchin, amp
  */
abstract class ResolvableCollection[T, +Member <: ResolvableCollectionMember[T]](
  private[run] var _defs: List[T],
  private var _lexicalContext: List[Binding],
  val runtime: OrcRuntime,
  val clock: Option[VirtualClock])
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

  //def buildFuture(i: Int): Future = new LocalFuture(runtime) 

  /** Create all the closures. They forward most of their methods here.
    */
  val members = definitions.indices.toList map (_ => new LocalFuture(runtime))

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

  /** State is used for the blocker side
    *
    */
  private var state: ResolvableCollectionState = Started

  override def setQuiescent(): Unit = {
    clock foreach { _.setQuiescent() }
  }
  override def unsetQuiescent(): Unit = {
    clock foreach { _.unsetQuiescent() }
  }

  /** Execute the resolution process of this Closure. This should be called by the scheduler.
    */
  override def run(): Unit = orc.util.Profiler.measureInterval(0L, 'ResolvableCollection_run) {
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
      case Resolved => {
        throw new AssertionError("Closure scheduled in bad state: " + state)
      }
    }
  }

  def isResolved = state match {
    case Resolved => true
    case _ => false
  }

  //// Blockable Implementation

  override def awakeNonterminalValue(v: AnyRef) = {
    // We only block on things that produce a single value so we don't have to handle multiple awakeNonterminalValue calls from one source.
    val bindings = v.asInstanceOf[List[Binding]]
    synchronized {
      _lexicalContext = bindings
      val realMembers = definitions.indices.toList map buildMember
      for ((f, v) <- members zip realMembers) {
        f.bind(v)
      }
      _context = realMembers.reverse.map { BoundValue(_) } ::: lexicalContext
      state = Resolved
      //waitlist
      //waitlist = Nil
    }
    //waits foreach runtime.stage
  }
  override def awakeStop() = throw new AssertionError("Should never halt")
  override def halt(): Unit = {} // Ignore halts

  def blockOn(b: Blocker) {
    assert(state != Resolved)
    state = Blocked(b)
  }

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
