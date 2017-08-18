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

import java.io.{ PrintWriter, StringWriter }
import java.util.concurrent.{ ConcurrentHashMap, LinkedBlockingDeque }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.logging.Level

import scala.annotation.elidable

import com.oracle.truffle.api.{ RootCallTarget, Truffle }
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.FrameInstance
import com.oracle.truffle.api.nodes.{ Node => TruffleNode }
import orc.ast.porc
import orc.run.porce.{ HasPorcNode, Logger }
import orc.util.Tracer

object Counter {
  import CounterConstants._

  val liveCounters = ConcurrentHashMap.newKeySet[Counter]()

  def exceptionString(e: Exception) = {
    val ss = new StringWriter()
    e.printStackTrace(new PrintWriter(ss))
    val s = ss.toString()
    s.dropWhile(_ != ':').drop(1)
  }

  @TruffleBoundary @noinline
  def getPorcStackTrace(): Seq[Either[porc.PorcAST, String]] = {
    val b = Seq.newBuilder[Either[porc.PorcAST, String]]
    def appendIfPorc(n: TruffleNode) = n match {
      case n: HasPorcNode =>
        n.porcNode match {
          case Some(n) =>
            b += Left(n)
          case None =>
            b += Right(n.toString())
        }
      case null =>
        {}
      case n =>
        b += Right(n.toString())
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

  val leavesOnly = true

  @TruffleBoundary @noinline
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
  @TruffleBoundary @noinline
  def addCounter(c: Counter) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.add(c)
    }
  }

  @elidable(elidable.ASSERTION)
  @TruffleBoundary @noinline
  def removeCounter(c: Counter) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      liveCounters.remove(c)
    }
  }

  val CounterNestedCreated = 101L
  Tracer.registerEventTypeId(CounterNestedCreated, "CtrNestC", _.formatted("%016x"), _.formatted("%016x"))
  val CounterTerminatorCreated = 102L
  Tracer.registerEventTypeId(CounterTerminatorCreated, "CtrTermC", _.formatted("%016x"), _.formatted("%016x"))
  val CounterServiceCreated = 103L
  Tracer.registerEventTypeId(CounterServiceCreated, "CtrServC", _.formatted("%016x"), _.formatted("%016x"))
}

/** A counter which tracks an executing part of the program.
  *
  * @author amp
  */
abstract class Counter protected (n: Int) extends AtomicInteger(n) {
  import CounterConstants._
  
  def this() = {
    this(1)
  }

  @elidable(elidable.ASSERTION)
  private val log = if (tracingEnabled) new LinkedBlockingDeque[Exception]() else null

  @elidable(elidable.ASSERTION)
  @TruffleBoundary @noinline
  protected final def logChange(s: => String) = {
    if (tracingEnabled && Logger.julLogger.isLoggable(Level.FINE)) {
      val stack = Counter.getPorcStackTrace().map(n => {
        n match {
          case Left(n) => 
            def rangeStr = n.sourceTextRange.map(_.lineContentWithCaret).getOrElse("")
            def nodeStr = n.toString().take(80)
            s"$rangeStr\n$nodeStr"
          case Right(s) =>
            s"[$s]"
        }
      }).mkString("\n---vvv---\n")
      log.add(new Exception(s"$s in Porc stack:\n$stack"))
    }
  }

  if (tracingEnabled) {
    logChange(s"Init to $n")
    Counter.addCounter(this)
  }

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

  final def setDiscorporate() = {
    //assert(get() > 0)
    isDiscorporated = true
  }

  final def discorporateToken() = {
    setDiscorporate()
    haltToken()
  }

  /** Decrement the count and check for overall halting.
    *
    * If we did halt call onContextHalted().
    */
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
      doHalt()
    }
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private final def doHalt() = {
    if (tracingEnabled) {
      //Logger.fine(s"Halted $this")
      //Counter.report()
      if (!isDiscorporated) {
        Counter.removeCounter(this)
      }
    }
    onHalt()
  }

  /** Increment the count.
    */
  final def newToken(): Unit = {
    val n = getAndIncrement()
    if (tracingEnabled) {
      logChange(s"+ Up from $n")
      assert(n >= 0, s"Spawning is not allowed once we go to zero count. No zombies allowed!!! $this")
    }
    if (n == 0) {
      doResurrect()
    }
  }

  @TruffleBoundary(allowInlining = true) @noinline
  private final def doResurrect() = {
    if (tracingEnabled) {
      Counter.addCounter(this)
      Logger.fine(s"Resurrected $this")
      //Counter.report()
      assert(isDiscorporated)
    }
    onResurrect()
  }

  override def toString(): String = {
    val n = getClass.getSimpleName
    val h = System.identityHashCode(this)
    f"$n%s@$h%x"
  }

  /** Called when this whole context has halted.
    *
    * This needs to be thread-safe.
    */
  def onHalt(): Unit

  /** Called when this context is resurrected and becomes alive again.
    *
    * This counter is being resurrected, so we need a new token from the parent.
    *
    * This needs to be thread-safe.
    */
  def onResurrect(): Unit
}

/** A Counter which forwards it's halting to a parent Counter and executes a closure on halt.
  *
  */
final class CounterNested(runtime: PorcERuntime, val parent: Counter, haltContinuation: PorcEClosure) extends Counter {
  //Tracer.trace(Counter.CounterNestedCreated, hashCode(), parent.hashCode(), 0)

  if (CounterConstants.tracingEnabled) {
    require(runtime != null)
    require(parent != null)
    require(haltContinuation != null)
  }

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
      runtime.schedule(CallClosureSchedulable(haltContinuation))
    } else {
      parent.setDiscorporate()
      // Token: from parent
      parent.haltToken()
    }
  }
}

/** A counter which always stays alive long enough to detect kills.
  *
  * This is specifically designed to handle calls into a Service methods.
  *
  */
final class CounterService(runtime: PorcERuntime, val parentCalling: Counter, val parentContaining: Counter, terminator: Terminator) extends Counter with Terminatable {
  //Tracer.trace(Counter.CounterServiceCreated, hashCode(), parentCalling.hashCode(), parentContaining.hashCode())

  if (CounterConstants.tracingEnabled) {
    require(runtime != null)
    require(terminator != null)
    require(parentCalling != null)
    require(parentContaining != null)
  }

  /** True if this counter has tokens from it's parents.
    */
  private val haveTokens = new AtomicBoolean(true)

  init()

  private def init() = {
    terminator.addChild(this)
    // Token: We were not passed a token for parentContaining so we need to get one.
    parentContaining.newToken()
  }

  def onResurrect() = {
    if (haveTokens.compareAndSet(false, true)) {
      parentCalling.newToken()
      init()
    }
  }

  private def haltParentToken() = {
    if (haveTokens.compareAndSet(true, false)) {
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
  * This is specifically designed to make sure terminators do not become garbage when there body halts.
  *
  */
final class CounterTerminator(runtime: PorcERuntime, val parent: Counter, terminator: Terminator) extends Counter with Terminatable {
  //Tracer.trace(Counter.CounterTerminatorCreated, hashCode(), parent.hashCode(), terminator.hashCode())

  if (CounterConstants.tracingEnabled) {
    require(runtime != null)
    require(terminator != null)
    require(parent != null)
  }

  /** True if this counter has tokens from it's parents.
    */
  private val state = new AtomicInteger(CounterTerminator.HasTokens)

  init()

  private def init() = {
    terminator.addChild(this)
  }

  def onResurrect() = {
    if (state.compareAndSet(CounterTerminator.HasNoTokens, CounterTerminator.HasTokens)) {
      parent.newToken()
      init()
    }
  }

  def onHalt(): Unit = {
    if (isDiscorporated) {
      if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.HasNoTokens)) {
        parent.discorporateToken()
      }
    } else {
      if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
        parent.haltToken()
        try {
          // Just make sure terminator is killed. If it already was ignore the exception.
          terminator.kill()
        } catch {
          case _: KilledException =>
            ()
        }
      }
    }
  }

  def kill() = {
    if (state.compareAndSet(CounterTerminator.HasTokens, CounterTerminator.WasKilled)) {
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
