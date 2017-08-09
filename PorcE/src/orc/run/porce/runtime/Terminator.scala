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
  
  @TruffleBoundary
  def addChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig == null) {
      child.kill()
    } else {
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
  
  @TruffleBoundary
  def removeChild(child: Terminatable): Unit = {
    val orig = children.get()
    if (orig != null) {
      orig.remove(child)
    }
  }

  /** Check that this context is live and throw KilledException if it is not.
    */
  def checkLive(): Unit = {
    if (!isLive()) {
      throw KilledException.SINGLETON
    }
  }

  /** Return true if this context is still live (has not been killed or halted
    * naturally).
    */
  def isLive(): Boolean = {
    children.get() != null
  }

  /** Kill the expressions under this terminator.
    *
    * This will throw KilledException if the terminator has already been killed otherwise it will just return to allow handling.
    */
  @TruffleBoundary(throwsControlFlowException = true)
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
  
  override def kill(): Unit = {
    parent.removeChild(this)
    super.kill()
  }
}
