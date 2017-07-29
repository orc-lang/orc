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
import scala.collection.JavaConverters._
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.run.porce.Logger

trait Terminatable {
  /** Kill this terminatable.
   *
   *  This method must be idempotent to multiple concurrent calls.
   */
  def kill(): Unit
}

/** A termination tracker.
  *
  * @author amp
  */
class Terminator extends Terminatable {
  private[this] var children = new AtomicReference(java.util.concurrent.ConcurrentHashMap.newKeySet[Terminatable]())
  
  @TruffleBoundary(allowInlining=true)
  def addChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig == null) {
      child.kill()
    } else {
      orig.add(child)
      
      /*
      val s = orig.size()
      if(s > 2000 && (s % 1000) == 0) {
        Logger.warning(s"ADDING: You may be leaking Terminatables: $this size=$s\nThis terminatable is $child\n----\n${orig.asScala.take(1000).mkString("\n")}")
      }
      */
      
      // Check for kill again.
      // The .add and .get here race against .getAndSet and iteration in kill().
      // However, this .get here will always return null if iteration will not observe the .add.
      // TODO: Someone please check this.
      if (children.get() == null) {
        child.kill()
      }
    }
  }
  
  @TruffleBoundary(allowInlining=true)
  def removeChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig != null) {
      /*val s = orig.size()
      if(s > 10000 && (s % 2000) == 0) {
        Logger.warning(s"REMOVING: You may be leaking Terminatables: $this size=$s\nThis terminatable is $child")
      }*/
      orig.remove(child)
    }
  }

  /** Check that this context is live and throw KilledException if it is not.
    */
  @TruffleBoundary(allowInlining = true)
  def checkLive(): Unit = {
    if (!isLive()) {
      throw KilledException.SINGLETON
    }
  }

  /** Return true if this context is still live (has not been killed or halted
    * naturally).
    */
  @TruffleBoundary(allowInlining = true)
  def isLive(): Boolean = {
    children.get() != null
  }

  /** Kill the expressions under this terminator.
    *
    * This will throw KilledException if the terminator has already been killed otherwise it will just return to allow handling.
    */
  @TruffleBoundary(allowInlining = true)
  def kill(): Unit = {
    // First, swap in null as the children set.
    val cs = children.getAndSet(null)
    // Next, process cs if needed.
    // See description of ordering in addChild().
    if (cs != null) {
      // If we were the first to kill and it succeeded
      for (c <- cs.asScala) {
        try {
          c.kill()
        } catch {
          case _: KilledException => {}
        }
      }
    } else {
      // If it was already killed
      throw KilledException.SINGLETON
    }
  }
}

/** A termination tracker which adds itself as a child of parent.
  *
  * @author amp
  */
final class TerminatorNested(parent: Terminator) extends Terminator {
  //Logger.info(s"$this($parent)")
  parent.addChild(this)
  
  @TruffleBoundary(allowInlining = true)
  override def kill(): Unit = {
    // FIXME: MEMORYLEAK: This is not actually enough. We actually need to detect halting of the elements in this terminators.... I think Counters and Terminators need to be connected.
    // Specifically this will be a problem if an expression halts without publishing inside a terminator. The optimizer will remove this for statically known cases.
    parent.removeChild(this)
    super.kill()
  }
}
