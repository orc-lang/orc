//
// EvalContext.scala -- Scala class/trait/object EvalContext
// Project OrcScala
//
// $Id$
//
// Created by amp on Jun 14, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.porc

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.ArrayList
import orc.run.StandardInvocationBehavior
import java.util.concurrent.LinkedBlockingDeque
import orc.OrcEvent
import orc.OrcExecutionOptions
import orc.Handle
import scala.util.parsing.input.NoPosition
import orc.run.extensions.RwaitEvent
import orc.error.OrcException
import orc.CaughtEvent
import orc.util.StackUtils

final case class Closure(body: Expr, var ctx: Context) {
  override def toString = s"Clos(${body.toStringShort})"
}

final class PorcHandle(private var pc: Closure, private var hc: Closure, private var terminator: Terminator, ctx: Context, private var interp: InterpreterContext) extends Handle with Terminable {
  def kill() = synchronized {
    if (terminator != null) {
      InterpreterContext.current(interp).schedule(hc)
      clear()
    }
  }

  def callSitePosition: scala.util.parsing.input.Position = NoPosition
  def setQuiescent(): Unit = {} // Ignore as we don't support virtual time

  def notifyOrc(event: OrcEvent): Unit = synchronized {
    event match {
      // TODO: There are problably other events I need to handle
      case RwaitEvent(d, h) => interp.engine.registerDelayed(d.toLong, () => h.publish())
      case _ =>
        ctx.eventHandler(event)
      //Logger.severe(event.toString)
    }
  }

  // FIXME: It would be much better to be able to make the call directly here. However 
  // that would require differentiating calls in the same thread and those in other threads. 
  // AND it would require a counter to prevent the stack from overflowing in deep recursion.
  def publish(v: AnyRef): Unit = synchronized {
    if (!isLive) return ;

    if (terminator.removeTerminable(this)) {
      Logger.finer(s"Site call published: -> $v   $this")
      /*if(startingThread == Thread.currentThread) {
          result = CallValue(v)
        } else*/
      InterpreterContext.current(interp).schedule(pc, List(v), halt = hc, trace = ctx.trace)
    }
    clear()
  }
  def halt: Unit = synchronized {
    if (!isLive) return ;

    if (terminator.removeTerminable(this)) {
      Logger.finer(s"Site call halted. $this")
      /*if(startingThread == Thread.currentThread) {
          result = CallHalt
        } else*/
      InterpreterContext.current(interp).schedule(hc, trace = ctx.trace)
    }
    clear()
  }

  def !!(e: OrcException): Unit = synchronized {
    if (!isLive) return ;

    notifyOrc(CaughtEvent(e))
    halt
  }

  def hasRight(rightName: String): Boolean = {
    ctx.options.hasRight(rightName)
  }

  def isLive: Boolean = {
    terminator != null && !terminator.isKilled
  }

  private def clear(): Unit = {
    terminator = null
    interp = null
    hc = null
    pc = null
  }
}

/** An object that can be terminated by terminators. They should use identity equality to
  * allow terminators to support removal correctly.
  */
trait Terminable {
  def kill(): Unit
}

final class Terminator {
  // Fields are public to allow inlining by the scala compiler, however they should not be directly accessed. 

  /** The collection of kill handlers. Null means this terminator has been killed.
    */
  var killHandlers = new ArrayList[Closure]()

  /** Similar to _killHandlers for terminables. They are called directly
    * (with the terminator lock held, instead of being handled in callKillHandlers).
    */
  var terminables = new ArrayList[Terminable]()

  Logger.fine(s"created $this")

  /** Is this terminator in the killed state?
    */
  def isKilled = synchronized {
    killHandlers == null
  }

  /** Kill this terminator and return the kill handlers if this is the first time it has been killed.
    */
  def setIsKilled(): Option[Seq[Closure]] = {
    import collection.JavaConversions._

    val r = this.synchronized {
      val khs = killHandlers
      if (khs != null) {
        killHandlers = null

        Some(khs.toSeq)
      } else {
        None
      }
    }

    if (r.isDefined) {
      // FIXME: Does thos actually need the defensive copy?
      for (t <- Seq() ++ terminables) {
        t.kill()
      }

      terminables = null
    }

    r
  }

  /** Add a kill handler if this is not killed. Return true if the handler was added,
    * false if this is already killed.
    */
  def addKillHandler(kh: Closure) = synchronized {
    if (isKilled) {
      false
    } else {
      killHandlers.add(kh)
      true
    }
  }

  /** Add a terminable to the list that will be called on kill. If the terminator
    * will be called return true. If not, return false. This second case occures
    * if this terminator is already killed.
    */
  def addTerminable(t: Terminable): Boolean = synchronized {
    if (!isKilled) {
      terminables.add(t)
      true
    } else {
      false
    }
  }
  /** Remove a terminable and return true if it was there before. This will return
    * false if the terminator has already COMPLETED it's kill on this terminable.
    * To simplify logic in the terminables during the kill processing this will
    * still return true.
    */
  def removeTerminable(t: Terminable): Boolean = synchronized {
    if (terminables != null)
      terminables.remove(t) // Returns true if the element was present in the list
    else
      false
  }
}

final class Counter {
  private[porc] val count: AtomicInteger = new AtomicInteger(1)

  Logger.fine(s"created $this")

  def increment() {
    val v = count.get()
    if (v <= 0) {
      Logger.info(s"Resurrecting old Counter $this ($v)")
      assert(false)
    } else {
      Logger.finer(s"incr $this ($v)") //:\n${StackUtils.getStack()}
      count.incrementAndGet()
    }
  }
  def decrementAndTestZero() = {
    val v = count.get()
    if (v <= 0) {
      Logger.info(s"Uberkilling old Counter $this ($v)")
      assert(false)
      false
    } else {
      Logger.finer(s"decr $this (${v - 1})") //:\n${StackUtils.getStack()}
      if (count.decrementAndGet() == 0) {
        synchronized {
          notifyAll()
        }
        true
      } else {
        false
      }
    }
  }

  @volatile
  var haltHandler: Closure = null
  /*
  def haltHandler = synchronized {
    assert(_haltHandler != null)
    Logger.finest(s"Getting halt $this, ${_haltHandler}")
    _haltHandler
  }
  def haltHandler_=(v: Closure) = synchronized { 
    assert(_haltHandler == null)
    Logger.finest(s"Setting halt $this, $v")
    _haltHandler = v
  }
  */

  private[porc] def waitZero() {
    while (count.get() != 0) synchronized { wait() }
  }
}

/** A store that encodes the state of a process in the interpreter.
  * @author amp
  */
final case class Context(valueStack: List[AnyRef], terminator: Terminator,
  counter: Counter, oldCounter: Counter, eventHandler: OrcEvent => Unit,
  options: OrcExecutionOptions, trace: Trace = Trace()) {
  def pushValue(v: Value) = copy(valueStack = v :: valueStack)
  def pushValues(vs: Seq[Value]) = copy(valueStack = vs.foldRight(valueStack)(_ :: _))
  @inline def getValue(i: Int) = valueStack(i)
  @inline def apply(i: Int) = getValue(i)

  def pushTracePosition(e: Expr) = {
    setTrace(trace + e)
  }
  def setTrace(t: Trace) = copy(trace = t)

  override def toString = {
    s"Context(terminator = $terminator, counter = $counter, oldCounter = $oldCounter, \n" ++
      valueStack.zipWithIndex.flatMap { v =>
        val v1s = if (v._1 == null) "null" else v._1.toString.replace('\n', ' ')
        s"${v._2}: $v1s\n"
      } ++ ")"
  }
}

final case class ScheduleItem(closure: Closure, args: List[AnyRef], halt: Closure, trace: Trace = Trace())

final class InterpreterContext(val engine: Interpreter) extends StandardInvocationBehavior {
  def schedule(clos: Closure, args: List[AnyRef] = Nil, halt: Closure = null, trace: Trace = Trace()) {
    schedule(ScheduleItem(clos, args, halt, trace))
  }
  def schedule(t: ScheduleItem) {
    queue.add(t)
  }
  def dequeue(): Option[ScheduleItem] = {
    val t = queue.poll()
    Option(t)
  }
  def steal(): Option[ScheduleItem] = {
    val t = queue.pollLast()
    Option(t)
  }

  // TODO: This needs to be a special dequeue implementation.
  val queue = new LinkedBlockingDeque[ScheduleItem]()

  def positionOf(e: Expr) = engine.debugTable(e)
  def positionOf(e: Var) = engine.debugTable(e)
}

object InterpreterContext {
  private val currentInterpreterContext = new ThreadLocal[InterpreterContext]()
  def current_=(v: InterpreterContext) = currentInterpreterContext.set(v)
  def current(ctx: InterpreterContext): InterpreterContext = {
    Option(currentInterpreterContext.get()) getOrElse ctx
  }
}
