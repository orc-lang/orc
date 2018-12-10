//
// Terminator.scala -- Scala class Terminator and related
// Project PorcE
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicReference

import orc.util.Tracer

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary

trait Terminatable {
  /** Kill this terminatable.
    *
    * This method must be idempotent and thread-safe, since it may be called times multiple concurrently.
    */
  def kill(): Unit
}

object Terminator {
  @inline
  val maxTerminatorDepth = CounterConstants.maxCounterDepth;

  val TerminatorAddChild = 150L
  Tracer.registerEventTypeId(TerminatorAddChild, "TrmAddCh", _.formatted("%016x"), _.formatted("%016x"))

  val TerminatorRemoveChild = 151L
  Tracer.registerEventTypeId(TerminatorRemoveChild, "TrmRemCh", _.formatted("%016x"), _.formatted("%016x"))
}

/** A termination tracker.
  *
  * @author amp
  */
class Terminator(val depth: Int) extends Terminatable {
  import Terminator._

  if (depth > maxTerminatorDepth) {
    throw new StackOverflowError(s"The Orc stack is limited to $maxTerminatorDepth. Make sure your functions are actually tail recursive.")
  }

  def this() = this(0)

  // FIXME: This should be made into a var which uses Unsafe (and eventually VarHandles) for atomic access.
  //         The overhead of the volitile reads seems to be small, but allowing the optimizer to merge
  //         checks (which would be non-volitile reads) would be good.
  protected[this] val children = new AtomicReference(java.util.concurrent.ConcurrentHashMap.newKeySet[Terminatable]())
  private var _isLive = true

  override def toString: String = s"${orc.util.GetScalaTypeName(this)}(children=${children.get})"

  /** Add a child to this terminator.
    *
    * All children are notified (with a kill() call) when the terminator is killed. child.kill may
    * be called during the call to addChild.
    */
  @TruffleBoundary(allowInlining = false) @noinline
  final def addChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig == null) {
      child.kill()
    } else {
      //Tracer.trace(TerminatorAddChild, 0, hashCode(), child.hashCode())
      orig.add(child)

      // This commented code should be enabled to run terminator_leak.orc
      /*
      val s = orig.size()
      if(s > 2000 && (s % 1000) == 0) {
        // \n----\n${orig.asScala.take(1000).mkString("\n")}
        Logger.warning(s"ADDING: You may be leaking Terminatables: $this size=$s, adding $child")
      }
      // */

      // Check for kill again.
      // The .add and .get here race against .getAndSet and iteration in kill().
      // However, this .get here will always return null if iteration will not observe the .add.
      // TODO: Someone please check this.
      if (children.get() == null) {
        child.kill()
      }
    }
  }

  /** Remove a child to this terminator.
    *
    * This is important due to memory management.
    */
  @TruffleBoundary(allowInlining = false) @noinline
  final def removeChild(child: Terminatable): Unit = {
    //Logger.log(Level.INFO, "", new Exception)
    val orig = children.get()
    if (orig != null) {
      //Tracer.trace(TerminatorRemoveChild, 0, hashCode(), child.hashCode())
      orig.remove(child)
    }
  }

  /** Check that this context is live and throw KilledException if it is not.
    */
  final def checkLive(): Unit = {
    if (!isLive()) {
      throw new KilledException()
    }
  }

  /** Return true if this context is still live (has not been killed or halted
    * naturally).
    */
  final def isLive(): Boolean = {
    _isLive // children.get() != null
  }

  /** Kill the expressions under this terminator.
    *
    * @param k The continuation to call if this is the first kill. `k` may be `null`, meaning that the continuation is a no-op.
    *
    * @return True iff the caller should call `k`; False iff the kill process will handle calling `k` or if it should not be called at all.
    * This return value allows the caller to call `k` more efficient (allowing inlining).
    *
    * This needs to be thread-safe and idempotent.
    *
    * Token: This call consumes a token on c if it returns false.
    */
  def kill(c: Counter, k: PorcEClosure): Boolean = {
    _isLive = false
    // First, swap in null as the children set.
    val cs = children.getAndSet(null)
    // Next, process cs if needed.
    // See description of ordering in addChild().
    if (cs != null) {
      // If we were the first to kill and it succeeded
      doKills(cs)
      true
    } else {
      if(c != null)
        c.haltToken()
      // If it was already killed
      false
    }
  }

  /** Kill the expressions under this terminator.
    *
    * This needs to be thread-safe and idempotent.
    */
  // FIXME: Move this method to TerminatorNested. No other terminator needs to be killed.
  def kill(): Unit = {
    kill(null, null)
  }

  @TruffleBoundary @noinline
  protected final def doKills(cs: java.util.concurrent.ConcurrentHashMap.KeySetView[Terminatable, java.lang.Boolean]) = {
    cs.forEach((c) =>
      try {
        c.kill()
      } catch {
        case _: KilledException => {}
      })
  }
}

// FIXME: MEMORYLEAK: Clean parent ref when killed.

/** A termination tracker which adds itself as a child of parent.
  *
  * @author amp
  */
final class TerminatorNested(parent: Terminator) extends Terminator(parent.depth + 1) {
  //Logger.info(s"$this($parent)")
  parent.addChild(this)

  override def kill(c: Counter, k: PorcEClosure): Boolean = {
    parent.removeChild(this)
    super.kill(c, k)
  }
}
