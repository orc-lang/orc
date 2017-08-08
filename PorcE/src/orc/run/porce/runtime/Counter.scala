//
// Counter.scala -- Scala class Counter
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.io.StringWriter
import java.io.PrintWriter
import java.util.concurrent.LinkedBlockingDeque
import scala.annotation.elidable
import orc.run.porce.Logger
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import orc.ast.porc
import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameInstance
import orc.run.porce.PorcENode
import com.oracle.truffle.api.RootCallTarget
import com.oracle.truffle.api.nodes.{ Node => TruffleNode }
import orc.run.porce.HasPorcNode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

object Counter {
  import CounterConstants._
  
  val liveCounters = ConcurrentHashMap.newKeySet[Counter]()

  def exceptionString(e: Exception) = {
    val ss = new StringWriter()
    e.printStackTrace(new PrintWriter(ss))
    val s = ss.toString()
    s.dropWhile(_ != ':').drop(1)
  }

  @TruffleBoundary
  def getPorcStackTrace(): Seq[porc.PorcAST] = {
    var b = Seq.newBuilder[porc.PorcAST]
    def appendIfPorc(n: TruffleNode) = n match {
      case n: HasPorcNode =>
        n.porcNode match {
          case Some(n) =>
            b += n
          case None =>
            Logger.fine(s"Found PorcE node without Porc node: $n")
        }
      case null =>
        {}
      case n =>
        Logger.fine(s"Found unknown node: $n")
    }
    Truffle.getRuntime.iterateFrames((f: FrameInstance) => {
      appendIfPorc(f.getCallNode)
      f.getCallTarget match {
        case c: RootCallTarget =>
          appendIfPorc(c.getRootNode)
        case t =>
          Logger.fine(s"Found unknown target: $t")
      }
    })
    b.result()
  }

  val leavesOnly = false

  @TruffleBoundary
  def report() = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      import scala.collection.JavaConverters._

      val allCounters = liveCounters.asScala
      lazy val parentCounters = allCounters.collect({
        case c: CounterNested =>
          c.parent
      }).toSet
      lazy val leafCounters = allCounters.filterNot(parentCounters)
      val counters = if (leavesOnly) leafCounters else allCounters

      Logger.fine(s"========================= Counter Report; showing ${counters.size} of ${allCounters.size} counters")
      Logger.fine("\n" + counters.map(c => s"$c: log size = ${c.log.size}, count = ${c.get}, isDiscorporated = ${c.isDiscorporated}").mkString("\n"))
      for (c <- counters) {
        Logger.fine(s"$c:\n${c.log.asScala.map(exceptionString(_)).mkString("---------------\n")}")
        c.log.clear()
      }
    } else {
      //Logger.warning(s"Cannot report Counter information if FINE is not loggable in ${Logger.julLogger.getName} or tracingEnabled == false")
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  def addCounter(c: Counter) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.add(c)
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary
  def removeCounter(c: Counter) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.remove(c)
    }
  }
}

/** A counter which tracks an executing part of the program.
 *  
 * @author amp
 */
abstract class Counter extends AtomicInteger(1) {
  import CounterConstants._
  
  @elidable(elidable.ASSERTION)
  val log = if (tracingEnabled) new LinkedBlockingDeque[Exception]() else null

  @elidable(elidable.ASSERTION)
  @TruffleBoundary(allowInlining = true)
  private final def logChange(s: => String) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      val stack = Counter.getPorcStackTrace().map(n => {
        val rangeStr = n.sourceTextRange.map(_.lineContentWithCaret).getOrElse("")
        val nodeStr = n.toString().take(80)
        s"$rangeStr\n$nodeStr"
      }).mkString("\n---vvv---\n")
      log.add(new Exception(s"$s in Porc stack:\n$stack"))
    }
  }
  logChange(s"Init to 1")

  Counter.addCounter(this)
  // */

  /*
  val log: LinkedBlockingDeque[Exception] = null
  @inline
  private def logChange(s: => String) = {
  }
  // */

  /*
   * The AtomicInteger we derive from is the number of executions that are 
   * either running or pending.
   *
   * This functions similarly to a reference count and this halts when count
   * reaches 0.
   */

  @volatile
  var isDiscorporated = false

  @TruffleBoundary(allowInlining = true)
  final def setDiscorporate() = {
    //assert(get() > 0)
    isDiscorporated = true
  }

  @TruffleBoundary(allowInlining = true)
  final def discorporateToken() = {
    setDiscorporate()
    haltToken()
  }

  /**
   * Decrement the count and check for overall halting.
   *
   * If we did halt call onContextHalted().
   */
  @TruffleBoundary(allowInlining = true)
  final def haltToken(): Unit = {
    val n = decrementAndGet()
    if (tracingEnabled) {
      logChange(s"- Down to $n")
      if (n < 0) {
        Counter.report()
      }
      assert(n >= 0, s"Halt is not allowed on already stopped Counters: $this")
    }
    if (n == 0) {
      if(tracingEnabled) {
        Logger.fine(s"Halted $this")
        Counter.report()
      }
      if (!isDiscorporated && tracingEnabled) {
        Counter.removeCounter(this)
      }
      onHalt()
    }
  }

  /**
   * Increment the count.
   */
  @TruffleBoundary(allowInlining = true)
  final def newToken(): Unit = {
    val n = getAndIncrement()
    if (n == 0) {
      if (tracingEnabled) {
        Logger.fine(s"Resurrected $this")
        Counter.report()
      }
      assert(isDiscorporated)
      Counter.addCounter(this)
      onResurrect()
    }
    if (tracingEnabled) {
      logChange(s"+ Up from $n")
      assert(n >= 0, s"Spawning is not allowed once we go to zero count. No zombies allowed!!! $this")
    }
  }
  
  override def toString(): String = {
    val n = getClass.getSimpleName
    val h = System.identityHashCode(this)
    f"$n%s@$h%x"
  }
    
  /**
   * Called when this whole context has halted.
   */
  def onHalt(): Unit
  
  /**
   * Called when this context is resurrected and becomes alive again.
   * 
   * This counter is being resurrected, so we need a new token from the parent.
   */
  def onResurrect(): Unit
}

/** A Counter which forwards it's halting to a parent Counter and executes a closure on halt.
 * 
 */
final class CounterNested(runtime: PorcERuntime, val parent: Counter, haltContinuation: PorcEClosure) extends Counter {
  require(runtime != null)
  require(parent != null)
  require(haltContinuation != null)
  
  def onResurrect() = {
    parent.newToken()
  }
  
  def onHalt(): Unit = {
    // Call the haltContinuation if we didn't discorporate.
    if (!isDiscorporated) {
      // TODO: PERFORMANCE: Instead of scheduling here consider notifying the caller to haltToken to invoke the closure. It's likely it would be a stable node which could be called directly.
      //   The tricky part will be handling the cases where instead of being called from a Truffle node we are called from the runtime.
      //   Maybe we need two ways to halt a token. One which schedules the handler and one which direct calls it.
      
      // Token: from parent
      runtime.scheduleOrCall(parent, () => {
        haltContinuation.callFromRuntime()
      })
    } else {
      parent.setDiscorporate()
      // Token: from parent
      parent.haltToken()
    }
  }
}

/** A counter which always stays alive long enough to detect kills.
 *  
 *  This is specifically designed to handle calls into a Service methods.
 *  
 */
final class CounterService(runtime: PorcERuntime, val parentCalling: Counter, val parentContaining: Counter, terminator: Terminator) extends Counter with Terminatable {
  require(runtime != null)
  require(terminator != null)
  require(parentCalling != null)
  require(parentContaining != null)
  
  /** True if this counter has tokens from it's parents.
   */
  private val haveTokens = new AtomicBoolean(true)
  
  init()
  
  private def init() = {
    // FIXME: Audit.
    terminator.addChild(this)
    // Token: We were not passed a token for parentContaining so we need to get one.
    parentContaining.newToken()
  }
  
  def onResurrect() = {
    // FIXME: Audit.
    if(haveTokens.compareAndSet(false, true)) {
      parentCalling.newToken()
      init()
    }
  }
  
  private def haltParentToken() = {
    // FIXME: Audit.
    if(haveTokens.compareAndSet(true, false)) {
      terminator.removeChild(this)
      // Token: We were passed a token at creation
      parentCalling.haltToken()
      // Token: We created a new token in the constructor.
      parentContaining.haltToken()
    }
  }
  
  def onHalt(): Unit = {
    if (!isDiscorporated) {
      haltParentToken()
    } else {
      // If we would discorporate instead just wait and see if a kill comes.
    }
  }
  
  def kill() = {
    haltParentToken()
  }
}


/** A counter which kills the terminator when it halts.
 *  
 *  This is specifically designed to make sure terminators do not become garbage when there body halts.
 *  
 */
final class CounterTerminator(runtime: PorcERuntime, val parent: Counter, terminator: Terminator) extends Counter with Terminatable {
  require(runtime != null)
  require(terminator != null)
  require(parent != null)
  
  /** True if this counter has tokens from it's parents.
   */
  private val state = new AtomicInteger(CounterTerminator.HasTokens)

  init()
  
  private def init() = {
    terminator.addChild(this)
  }
  
  def onResurrect() = {
    // FIXME: Audit.
    if(state.compareAndSet(CounterTerminator.HasNoTokens, CounterTerminator.HasTokens)) {
      parent.newToken()
      init()
    }
  }
  
  def onHalt(): Unit = {
    // FIXME: Audit.
      if (isDiscorporated) {
        if(state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.HasNoTokens)) {
            parent.discorporateToken()
        }
      } else {
        if(state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
          parent.haltToken()
          terminator.kill()
        }
      }
  }
  
  def kill() = {
    // FIXME: Audit.
    if(state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
      // Do nothing.
      // The token we have is passed to the invoker of terminator.kill()
    }
  }
}

object CounterTerminator {
  val HasNoTokens = 0
  val HasTokens = 1
  val WasKilled = 2
}
