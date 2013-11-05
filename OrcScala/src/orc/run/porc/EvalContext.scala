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

final case class Closure(body: Expr, var ctx: Context) {
  override def toString = s"Clos(${body.toString.replace('\n', ' ').take(100)})"
}

final class PorcHandle(private var pc: Closure, private var hc: Closure, private var terminator: Terminator, ctx: Context, private var interp: InterpreterContext) extends Handle {
  // TODO: It would probably be faster to use an atomic boolean.
  
  def kill() = synchronized {
    if (isLive) {
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

    //Logger.finer(s"Site call published: $callable $arguments -> $v")
    /*if(startingThread == Thread.currentThread) {
          result = CallValue(v)
        } else*/
    InterpreterContext.current(interp).schedule(pc, List(v), halt = hc)
    clear()
  }
  def halt: Unit = synchronized {
    if (!isLive) return ;

    //Logger.finer(s"Site call halted: $callable $arguments")
    /*if(startingThread == Thread.currentThread) {
          result = CallHalt
        } else*/
    InterpreterContext.current(interp).schedule(hc)
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

final class Terminator {
  // Fields are public to allow inlining by the scala compiler, however they should not be directly accessed. 

  val _isKilled = new AtomicBoolean(false)
  def isKilled = _isKilled.get()
  def setIsKilled(): Boolean = _isKilled.getAndSet(true) == false

  Logger.fine(s"created $this")

  var _killHandlers = new ArrayList[Closure]()
  def addKillHandler(kh: Closure) = _killHandlers synchronized {
    if(_killHandlers == null) {
      false
    } else {
      _killHandlers.add(kh)
      true
    }
  }
  def killHandlers() = _killHandlers synchronized {
    assert(isKilled)
    import collection.JavaConversions._
    val ret = _killHandlers.toList
    _killHandlers = null
    ret
  }
}

final class Counter {
  private[porc] val _count: AtomicInteger = new AtomicInteger(1)

  Logger.fine(s"created $this")

  def increment() {
    val v = _count.get()
    if (v <= 0) {
      assert(v > 0, "Resurrecting old Counter")
    }
    Logger.finer(s"incr $this ($v)")
    _count.incrementAndGet()
  }
  def decrementAndTestZero() = {
    val v = _count.get()
    if (v <= 0) {
      assert(v > 0, "Uberkilling old Counter")
    }
    Logger.finer(s"decr $this ($v)")
    if (_count.decrementAndGet() == 0) {
      synchronized {
        notifyAll()
      }
      true
    } else {
      false
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
    while (_count.get() != 0) synchronized { wait() }
  }
}

/** A store that encodes the state of a process in the interpreter.
  * @author amp
  */
final case class Context(valueStack: List[AnyRef], terminator: Terminator,
  counter: Counter, oldCounter: Counter, eventHandler: OrcEvent => Unit,
  options: OrcExecutionOptions) {
  def pushValue(v: Value) = copy(valueStack = v :: valueStack)
  def pushValues(vs: Seq[Value]) = copy(valueStack = vs.foldRight(valueStack)(_ :: _))
  @inline def getValue(i: Int) = valueStack(i)
  @inline def apply(i: Int) = getValue(i)

  override def toString = {
    s"Context(terminator = $terminator, counter = $counter, oldCounter = $oldCounter, \n" ++
      valueStack.zipWithIndex.flatMap { v =>
        val v1s = if (v._1 == null) "null" else v._1.toString.replace('\n', ' ')
        s"${v._2}: $v1s\n"
      } ++ ")"
  }
}

final class InterpreterContext(val engine: Interpreter) extends StandardInvocationBehavior {
  def schedule(clos: Closure, args: List[AnyRef] = Nil, halt: Closure = null) {
    queue.add((clos, args, halt))
  }
  def schedule(t: (Closure, List[AnyRef], Closure)) {
    queue.add(t)
  }
  def dequeue(): Option[(Closure, List[AnyRef], Closure)] = {
    Option(queue.poll())
  }
  def steal(): Option[(Closure, List[AnyRef], Closure)] = {
    Option(queue.pollLast())
  }

  // TODO: This needs to be a special dequeue implementation.
  val queue = new LinkedBlockingDeque[(Closure, List[AnyRef], Closure)]()
}

object InterpreterContext {
  private val currentInterpreterContext = new ThreadLocal[InterpreterContext]()
  def current_=(v: InterpreterContext) = currentInterpreterContext.set(v)
  def current(ctx: InterpreterContext): InterpreterContext = {
    Option(currentInterpreterContext.get()) getOrElse ctx
  }
}
