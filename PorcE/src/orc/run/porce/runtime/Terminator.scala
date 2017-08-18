//
// Terminator.scala -- Scala class Terminator and related
// Project OrcScala
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicReference

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.util.Tracer

trait Terminatable {
  /** Kill this terminatable.
    *
    * This method must be idempotent and thread-safe, since it may be called times multiple concurrently.
    */
  def kill(): Unit
}

object Terminator {
  val TerminatorAddChild = 150L
  Tracer.registerEventTypeId(TerminatorAddChild, "TrmAddCh", _.formatted("%016x"), _.formatted("%016x"))
}

/** A termination tracker.
  *
  * @author amp
  */
class Terminator extends Terminatable {
  //import Terminator._

  protected[this] var children = new AtomicReference(java.util.concurrent.ConcurrentHashMap.newKeySet[Terminatable]())

  /** Add a child to this terminator.
    *
    * All children are notified (with a kill() call) when the terminator is killed. child.kill may
    * be called during the call to addChild.
    */
  @TruffleBoundary @noinline
  final def addChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig == null) {
      child.kill()
    } else {
      //Tracer.trace(TerminatorAddChild, hashCode(), child.hashCode(), 0)
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
  @TruffleBoundary @noinline
  final def removeChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig != null) {
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
    children.get() != null
  }

  /** Kill the expressions under this terminator.
    *
    * @param k The continuation to call if this is the first kill. `k` may be `null`, meaning that the continuation is a no-op.
    *
    * @return True iff the caller should call `k`; False iff the kill process will handle calling `k` or if it should not be called at all.
    * This return value allows the caller to call `k` more efficient (allowing inlining).
    *
    * This needs to be thread-safe and idempotent.
    */
  def kill(c: Counter, k: PorcEClosure): Boolean = {
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

/** A termination tracker which adds itself as a child of parent.
  *
  * @author amp
  */
final class TerminatorNested(parent: Terminator) extends Terminator {
  //Logger.info(s"$this($parent)")
  parent.addChild(this)

  override def kill(c: Counter, k: PorcEClosure): Boolean = {
    parent.removeChild(this)
    super.kill(c, k)
  }
}
